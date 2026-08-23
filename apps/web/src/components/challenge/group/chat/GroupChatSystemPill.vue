<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { clockLabel, trialPillView } from '@/utils/groupChat';
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

    /*
     * 같은 분에 알림이 여러 줄 떨어지면 마지막 줄에만 시각을 남긴다 —
     * 판정은 화면이 「다음 줄」을 보고 한다(utils/groupChat 의 shouldShowTime).
     * 기본값이 true 라 이 필을 단독으로 써도 시각이 사라지지 않는다.
     */
    showTime: { type: Boolean, default: true },
});

const router = useRouter();

const view = computed(() => trialPillView(props.message));

/*
 * 왼쪽 원. 판결이 나면 탕이 얼굴 대신 결과 도장이 들어간다.
 *
 * alt 를 비우지 않는다. 옆 문구가 「유죄예요」를 이미 말하고 있으니 중복처럼 보이지만,
 * 그 문구는 서버가 만드는 값이라 문장이 바뀌면 도장만 남는다 — 결과를 그림으로만 말하게 된다.
 * (원래 GroupChatVerdictCard 가 쓰던 값 그대로다)
 */
const STAMPS = {
    GUILTY: { src: guiltyStamp, alt: '유죄 판결 인장' },
    INNOCENT: { src: innocentStamp, alt: '무죄 판결 인장' },
};

const avatar = computed(() => {
    if (view.value.stamp) return STAMPS[view.value.stamp];

    return props.message.systemType === 'VERDICT_CONFIRMED'
        ? { src: verdictImg, alt: '판사 탕이' }
        : { src: judgingImg, alt: '판사 탕이' };
});

/* 말풍선과 같은 자리·같은 모양(utils/groupChat 의 clockLabel). sentAt 이 없으면 빈 문자열이다 */
const timeLabel = computed(() => clockLabel(props.message.sentAt));

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
                >{{ view.lead }}</span
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

        <!--
          시각은 필 <b>아래</b>다. 말풍선처럼 옆에 붙이면 가운데 정렬인 필이 그만큼 한쪽으로
          밀려 줄마다 중심이 흔들린다. 필 안에 넣는 것도 아니다 — 좁은 한 줄에서 문구가 더 잘린다.

          showTime 이 거짓이면 자리째 없앤다. 적발·개시가 같은 초에 붙어 오므로 줄마다
          같은 시각을 찍으면 필 아래가 같은 숫자로 두 번 채워진다.
        -->
        <span v-if="showTime && timeLabel" class="sys-pill__time">{{ timeLabel }}</span>
    </div>
</template>

<style scoped>
/*
 * 등장 애니메이션은 합치기 전 기록·판결 카드가 쓰던 것을 그대로 가져왔다 — 재판 알림은 내가
 * 보낸 것이 아닌데 대화 중간에 끼어들므로, 아래에서 밀려 올라오면 「방금 도착했다」가 읽힌다.
 * `both` 라 끝난 뒤 마지막 프레임에 머문다. 마운트될 때 한 번만 돈다.
 */
.sys-pill-wrap {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 3px;
    padding: var(--tt-space-1) 0;
    animation: sys-pill-enter 0.28s ease-out both;
}

@keyframes sys-pill-enter {
    from {
        opacity: 0;
        transform: translateY(8px);
    }
}

/* 움직임에 어지러움을 느끼는 사용자가 있다. 시스템 설정을 켜 두면 결과 프레임만 남긴다 */
@media (prefers-reduced-motion: reduce) {
    .sys-pill-wrap {
        animation: none;
    }
}

/*
 * 크기·색은 말풍선의 시각과 같지만(GroupChatBubble 의 .bubble-row__time) <b>자리가 다르다.</b>
 * 말풍선은 왼쪽·오른쪽으로 갈려 있어 시각을 옆에 붙이면 누가 보냈는지가 같이 읽히는데,
 * 재판 알림 필은 가운데 정렬이라 옆에 붙이면 필이 그만큼 한쪽으로 밀린다.
 */
.sys-pill__time {
    flex: none;
    white-space: nowrap;
    color: var(--tt-text-hint);
    font-size: var(--tt-fs-overline);
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
    overflow-wrap: break-word;
    color: var(--pill-text);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
    line-height: 1.4;
}

/*
 * 닉네임. inline-block + max-width:100% 라 한 줄을 다 쓰면 자기 자리에서 ellipsis 로 줄고,
 * 뒤에 오는 문장은 잘리지 않는다.
 *
 * 닉네임과 문장을 가르는 것은 <b>굵기뿐</b>이다. 한때 뒷문장에 `opacity: 0.78` 을 걸었다가
 * 되돌렸다 — 필 색이 다섯 종이라 배경마다 대비가 다르게 깎인다. gold 는 4.56 → 3.06,
 * danger 는 4.11 → 2.97 로 12px 본문의 AA 기준(4.5)을 크게 밑돌았다.
 * --tt-accent-warn 은 주석부터가 「골드소프트 배경 위 경고 텍스트」라 그 배경에 맞춰 잡아 둔
 * 값인데, 투명도를 얹는 순간 디자인시스템이 계산해 둔 짝이 깨진다.
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
