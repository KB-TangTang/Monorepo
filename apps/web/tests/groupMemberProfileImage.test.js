import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { registerHooks } from 'node:module';

/*
 * 참여자 프로필 이미지가 화면까지 닿는지 (이슈 #407).
 *
 * 프로필을 바꿔도 그룹 상세 멤버 그리드와 채팅 아바타가 계속 이니셜만 그렸다.
 * 원인은 **서버 이름과 화면 이름이 다른데 그 변환이 members 에만 빠져 있던 것**이다.
 * 서버는 Lombok 이 정한 `profileImageUrl` 을 주고 화면은 `profileImage` 를 읽는데,
 * `toViewModel` 의 members 매핑이 그 줄만 없었다.
 *
 * 🔴 **이 테스트가 없어서 한 번 놓쳤다.** 스토어 테스트를 6건 붙였는데도 못 잡았다 —
 *   스텁이 서버가 주지도 않는 `{ userId, profileImage }` 모양을 지어내 반환했기 때문이다.
 *   가짜 응답 위에서 돌린 검증은 이름이 어긋난 것을 볼 수 없다.
 *   그래서 여기서는 **서버가 실제로 주는 이름**(`profileImageUrl`)을 넣고 변환을 확인한다.
 */

const HTTP_STUB = new URL('./stubs/httpStub.js', import.meta.url).href;
/* devDataSource 는 import.meta.env.DEV 를 읽는다 — Vite 밖에서는 import 만 해도 터진다 */
const DEV_STUB = new URL('./stubs/devDataSourceStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/http') {
            return { url: HTTP_STUB, shortCircuit: true };
        }
        if (specifier === '@/services/devDataSource') {
            return { url: DEV_STUB, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const http = await import(HTTP_STUB);
const { fetchGroupDetail } = await import('../src/api/groupChallenge.js');

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

test('서버의 profileImageUrl 을 화면이 읽는 profileImage 로 바꾼다', async () => {
    http.reset();
    http.setGetResponse({
        id: 3,
        members: [
            { userId: 7, nickname: '탕이', profileImageUrl: 'https://img/7.png', owner: true },
            { userId: 9, nickname: '재판장', profileImageUrl: null, owner: false },
        ],
    });

    const detail = await fetchGroupDetail(3);

    assert.equal(detail.members[0].profileImage, 'https://img/7.png');
    // 이미지가 없는 참여자는 null 이어야 한다 — UserAvatar 가 이니셜로 그린다
    assert.equal(detail.members[1].profileImage, null);
});

test('members 가 없어도 터지지 않는다', async () => {
    http.reset();
    http.setGetResponse({ id: 3 });

    const detail = await fetchGroupDetail(3);

    assert.deepEqual(detail.members, []);
});

/*
 * 서버가 이 필드를 실제로 채우는지는 백엔드 테스트가 본다
 * (ChallengeGroupServiceTest — 「상세의 참여자 목록에 프로필 이미지 URL 이 담긴다」).
 * 여기서는 DTO 에 필드가 살아 있는지만 확인한다. 지워지면 변환이 조용히 null 이 된다.
 */
test('서버 DTO 에 profileImageUrl 필드가 있다', () => {
    assert.match(
        source('../api/src/main/java/com/kb/tangtang/challenge/dto/GroupMemberDto.java'),
        /private String profileImageUrl;/,
    );
});

test('멤버 그리드와 채팅 버블이 그 값을 아바타로 넘긴다', () => {
    assert.match(
        source('src/components/challenge/group/GroupDetailMemberGrid.vue'),
        /:image-url="m\.profileImage"/,
    );
    assert.match(
        source('src/views/challenge/group/GroupChatView.vue'),
        /:avatar-url="store\.imageOf\(item\.data\.senderId\)"/,
    );
});
