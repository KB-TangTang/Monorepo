<!--
  용도: 재판탭 두 홈 화면(개인 · 그룹)이 같이 쓰는 다크 헤더.
        법원 건물 일러스트를 폭 전체에 깔고 그 위에 알림 벨,
        아래에 부제 + 탕이 + 양피지 말풍선을 얹는다.
        말풍선 머리(판사봉 엠블럼 오른쪽 빈칸)에 화자 이름이 들어간다.
  언제 쓰는지: /mission/personal · /group-challenges 최상단.
  쓰면 안 되는 경우: 재판탭 하위 화면. 상세 화면은 ChallengePageHeader 를 쓴다.

  **두 화면의 헤더 높이는 완전히 같다.** 높이에 관여하는 값이 전부 고정이기 때문이다 —
  hero 는 aspect-ratio, 말풍선도 aspect-ratio, 탕이는 고정 height.
  variant 는 색과 이미지 배치만 바꾼다. 자세한 계산은 ChallengeCourtHeader.css 참고.
-->
<script setup>
import TheNotificationBell from '@/components/common/TheNotificationBell.vue';
import courtSpeechBubble from '@/assets/images/court/court_speech_bubble.png';

defineProps({
    /** 'supreme'(개인챌린지 · 대법원) | 'district'(그룹챌린지 · 지방법원) */
    variant: {
        type: String,
        default: 'supreme',
        validator: (value) => ['supreme', 'district'].includes(value),
    },
    courtImage: { type: String, required: true },
    courtAlt: { type: String, required: true },
    subtitle: { type: String, required: true },
    tangiImage: { type: String, required: true },
    /** 말풍선 머리에 이름 앞으로 붙는 직함. 개인은 '검사', 그룹은 '판사'. */
    tangiRole: { type: String, required: true },
    /** 개인챌린지는 난이도로 고른 검사 이름이 그대로 들어온다 ('냉정한 탕이' 같은 것). */
    tangiName: { type: String, required: true },
    /** 말풍선 본문. 줄바꿈은 \n 으로 넣는다 (pre-line 으로 그린다). */
    quote: { type: String, default: '' },
    /**
     * 헤더 하단 곡면에 얹는 첫 섹션 제목 ('오늘의 미션' 같은 것).
     * **비워도 곡면 높이는 그대로다.** 두 화면의 헤더 높이가 어긋나지 않게 고정해 뒀다.
     */
    skirtTitle: { type: String, default: '' },
});
</script>

<template>
    <header class="court-header" :class="`court-header--${variant}`">
        <div class="court-header__statusbar"></div>

        <div class="court-header__hero">
            <img :src="courtImage" :alt="courtAlt" class="court-header__hero-img" />
            <div class="court-header__bell">
                <TheNotificationBell />
            </div>
        </div>

        <div class="court-header__foot">
            <p class="court-header__subtitle">{{ subtitle }}</p>
            <div class="court-header__talk">
                <img :src="tangiImage" alt="" class="court-header__tangi" />
                <div
                    class="court-header__speech"
                    :style="{ backgroundImage: `url(${courtSpeechBubble})` }"
                >
                    <span class="court-header__speech-name">{{ tangiRole }} {{ tangiName }}</span>
                    <p class="court-header__speech-text">{{ quote }}</p>
                </div>
            </div>
        </div>

        <!--
          다크 헤더와 밝은 본문 사이를 이어주는 곡면. 본문 배경색과 같아야 한다.
          첫 섹션 제목이 여기 앉는다 — 본문에 또 제목 줄을 두면 그만큼 화면이 밀린다.
        -->
        <div class="court-header__skirt">
            <h2 v-if="skirtTitle" class="court-header__skirt-title">{{ skirtTitle }}</h2>
        </div>
    </header>
</template>

<style scoped src="./ChallengeCourtHeader.css"></style>
