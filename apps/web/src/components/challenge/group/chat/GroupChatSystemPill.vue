<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { trialPillView } from '@/utils/groupChat';
import judgingImg from '@/assets/images/emotions/48_judging.png';
import verdictImg from '@/assets/images/emotions/49_verdict.png';
import guiltyStamp from '@/assets/images/judgment/guilty_stamp.png';
import innocentStamp from '@/assets/images/judgment/innocent_stamp.png';

/*
 * 재판 알림 필 — 적발 · 개시 · 변론 · 판결을 한 줄로 그린다 (이슈 #452).
 *
 * 카드 세 종(GroupChatRecordCard · GroupChatVerdictCard · 폴백 pill)이 여기로 합쳐졌다.
 * 카드는 세로로 62px 도장과 제목·본문·사건번호를 쌓아 대화 흐름을 매번 끊었는데,
 * 기소 한 건이 적발·개시·변론·판결로 네 번 오므로 방이 카드로 덮였다.
 *
 * <b>제목(「소비가 적발됐습니다」 같은 프론트 전용 한국어 라벨)이 사라졌다.</b>
 * 그 자리를 색과 버튼 문구가 대신한다 — Ink+사건 보기 / Gold+투표하기 /
 * Ink(버튼 없음) / Red·Green+판결문 네 조합이 서로 겹치지 않는다.
 *
 * 표시 규칙은 utils/groupChat 의 trialPillView 에 있다. 이 저장소에는 SFC 렌더링
 * 하네스가 없어 컴포넌트 안에 두면 검증이 안 된다.
 */
const props = defineProps({
    message: { type: Object, required: true },
});

const router = useRouter();

const view = computed(() => trialPillView(props.message));

/*
 * 왼쪽 원. 판결이 나면 탕이 얼굴 대신 결과 도장이 들어간다.
 *
 * 도장을 띄울 때 alt 를 비우는 것은 도장이 말하는 「유죄·무죄」를 바로 옆 문구가 이미 말하고
 * 있어서다 — 읽어 주는 쪽에는 같은 말이 두 번 들린다. 보는 쪽에는 색·문구·도장 세 겹이라
 * 색만으로 결과를 가려내지 않아도 된다.
 */
const STAMPS = { GUILTY: guiltyStamp, INNOCENT: innocentStamp };

const avatar = computed(() => {
    if (view.value.stamp) return { src: STAMPS[view.value.stamp], alt: '' };

    return props.message.systemType === 'VERDICT_CONFIRMED'
        ? { src: verdictImg, alt: '판사 탕이' }
        : { src: judgingImg, alt: '판사 탕이' };
});

function openCta() {
    router.push(view.value.ctaTo);
}
</script>

<template>
    <div class="sys-pill-wrap">
        <div class="sys-pill" :class="`sys-pill--${view.tone}`">
            <img
                class="sys-pill__avatar"
                :class="{ 'sys-pill__avatar--stamp': view.stamp }"
                :src="avatar.src"
                :alt="avatar.alt"
            />

            <!--
              min-width:0 이 없으면 줄어들지 않는다. flex 자식의 기본값이 min-width:auto 라
              내용보다 작아지지 못해, ellipsis 대신 필이 통째로 화면 밖으로 나간다.

              닉네임과 나머지를 두 조각으로 나눠 그리는 이유는 utils/groupChat 의 splitName 에 있다 —
              줄어드는 건 닉네임 쪽이고 문장은 끝까지 남는다. 줄바꿈 자리에 공백이 끼지 않게
              두 span 사이를 붙여 쓴다.
            -->
            <span class="sys-pill__lead"
                ><b v-if="view.leadName" class="sys-pill__name">{{ view.leadName }}</b
                ><span class="sys-pill__rest">{{ view.lead }}</span></span
            >

            <!-- 화살표는 장식이다. 스크린리더가 「단일 오른쪽 홑화살괄호」를 읽지 않게 숨긴다 -->
            <button
                v-if="view.ctaLabel"
                class="sys-pill__cta"
                :class="{ 'sys-pill__cta--quiet': view.ctaQuiet }"
                @click="openCta"
            >
                {{ view.ctaLabel }}<span v-if="view.ctaQuiet" aria-hidden="true"> ›</span>
            </button>
        </div>
    </div>
</template>

<style scoped>
.sys-pill-wrap {
    display: flex;
    justify-content: center;
    padding: var(--tt-space-1) 0;
}

/*
 * 모서리는 말풍선과 같은 --tt-radius-lg 다. 시안의 Ink 필이 16px 인데 우리 토큰에 그 값이 없고,
 * 18px 이 GroupChatBubble 과 같아 같은 화면에서 곡률이 따로 놀지 않는다.
 * (완전 라운드 999px 은 시안에서 Gold·Red 필만 쓰던 값이라 4종에 다 적용한 것이 잘못이었다)
 */
.sys-pill {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    max-width: 320px;
    background: var(--pill-bg);
    border: 1px solid var(--pill-border);
    border-radius: var(--tt-radius-lg);
    padding: 7px 9px;
}

.sys-pill__avatar {
    flex-shrink: 0;
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: var(--tt-white);
    object-fit: contain;
}

/*
 * 판결 도장은 아바타보다 크다. 눈으로 정한 값이 아니라 427px 원본을 실제로 줄여 보고 정했다 —
 * 30px 에서는 「유죄」·「무죄」가 뭉개져 색 얼룩이 되고 40px 부터 한글이 읽힌다.
 * 아래 라틴 줄(GUILTY / NOT GUILTY)은 어느 크기에서도 살아나지 않으므로 포기한 정보다.
 *
 * 이 10px 때문에 판결 필만 다른 셋보다 살짝 높아진다. 판결은 기소 한 건의 결말이라
 * 줄 높이가 똑같지 않은 편이 오히려 맞다고 봤다.
 */
.sys-pill__avatar--stamp {
    width: 40px;
    height: 40px;
    background: none;
}

/*
 * 두 줄까지 허용한다. 360px 에서 텍스트 폭이 한 줄 약 13자인데 적발 문구는 닉네임 2자에서도
 * 17자다 — 한 줄로 고정하면 사실상 모든 알림이 잘린다(브라우저에서 확인하고 뒤집은 결정).
 * 두 줄이면 26자가 들어와 닉네임 10자대까지 온전히 보인다.
 *
 * 그래도 넘치는 경우(닉네임은 50자까지 가능하다)를 위해 줄어드는 쪽을 닉네임으로 못박는다 —
 * 아래 __name 참고. JS 로 자르지 않는다: 폰트·화면 폭에 따라 어긋나고 잘린 자리가 데이터가 된다.
 */
.sys-pill__lead {
    min-width: 0;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    color: var(--pill-text);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
    line-height: 1.4;
}

/*
 * 닉네임. inline-block + max-width:100% 라 한 줄을 다 쓰면 자기 자리에서 ellipsis 로 줄고,
 * 뒤에 오는 문장은 잘리지 않는다.
 */
.sys-pill__name {
    display: inline-block;
    max-width: 100%;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    vertical-align: bottom;
    font-weight: var(--tt-fw-black);
}

/*
 * 나머지 문장은 한 단계 낮춘다. 색 토큰을 새로 만들지 않고 투명도로 낮추는 이유는
 * 필 색이 다섯 종(ink·gold·danger·success·muted)이라 각각의 「연한 글자색」을 따로 두면
 * 토큰이 다섯 개 늘기 때문이다. 대비는 어느 배경에서든 같은 비율로 유지된다.
 */
.sys-pill__rest {
    opacity: 0.78;
}

/*
 * 모서리는 --tt-radius-sm(12px) 이다. 중첩 반경은 「바깥 − 여백」이라야 두 곡선이 나란히 도는데
 * 여기가 18 − 7 = 11px 이고 그 값에 가장 가까운 토큰이 sm 이다(주석도 「내부 요소」).
 * 필이 통째로 999px 이던 시절엔 버튼도 999px 이라 맞아떨어졌지만, 바깥만 18px 로 바꾸면서
 * 안쪽이 훨씬 둥근 채로 남아 버튼이 필 위에 얹힌 것처럼 보였다.
 */
.sys-pill__cta {
    flex-shrink: 0;
    border: none;
    cursor: pointer;
    white-space: nowrap;
    background: var(--pill-cta-bg);
    color: var(--pill-cta-text);
    font-family: inherit;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 5px 11px;
    border-radius: var(--tt-radius-sm);
}

/*
 * 판결문. 배경을 빼고 문구와 같은 크기·색으로 둔다 — 이유는 utils/groupChat 의 QUIET_CTA 에 있다.
 * padding 을 0 으로 두는 시안과 달리 세로 여백은 남긴다. 누를 자리가 글자 높이만큼밖에 없으면
 * 손가락으로 맞히기 어렵다.
 */
.sys-pill__cta--quiet {
    background: none;
    color: var(--pill-text);
    font-size: var(--tt-fs-caption);
    padding: 5px 2px;
}

.sys-pill__cta:active {
    transform: translateY(1px);
}

.sys-pill__cta:focus-visible {
    outline: 2px solid var(--tt-accent-bright);
    outline-offset: 2px;
}

/* ── 색 ─────────────────────────────────────────────────
   판결은 무죄를 초록으로 둔다. 한 줄 필에서는 색이 유일한 결과 신호라
   무죄에 붉은 색을 쓰면 정보가 뒤집힌다.

   아래 셋(danger·success·muted)에 --pill-cta-* 가 없는 것은 빠뜨린 게 아니다.
   이 세 색은 판결 필에만 쓰이고 판결의 버튼은 --quiet 라 배경을 쓰지 않는다. */
.sys-pill--ink {
    --pill-bg: var(--tt-surface-inverse);
    --pill-border: var(--tt-surface-inverse);
    --pill-text: var(--tt-text-inverse);
    --pill-cta-bg: var(--tt-accent);
    --pill-cta-text: var(--tt-ink);
}

.sys-pill--gold {
    --pill-bg: var(--tt-accent-subtle);
    --pill-border: var(--tt-accent-subtle-border);
    --pill-text: var(--tt-accent-warn);
    --pill-cta-bg: var(--tt-ink);
    --pill-cta-text: var(--tt-accent);
}

.sys-pill--danger {
    --pill-bg: var(--tt-danger-subtle);
    --pill-border: var(--tt-danger-subtle-border);
    --pill-text: var(--tt-danger-deep);
}

.sys-pill--success {
    --pill-bg: var(--tt-success-subtle);
    --pill-border: var(--tt-success-subtle-border);
    --pill-text: var(--tt-success-deep);
}

/* systemType 이 없던 시절의 메시지. Redis TTL(종료일 + 2일) 동안 계속 흘러나온다 */
.sys-pill--muted {
    --pill-bg: var(--tt-bg-fill);
    --pill-border: var(--tt-bg-fill);
    --pill-text: var(--tt-text-muted);
}
</style>
