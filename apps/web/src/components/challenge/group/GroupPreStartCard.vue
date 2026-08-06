<script setup>
defineProps({
    challenge: { type: Object, required: true },
});
</script>

<template>
    <div class="gps-card">
        <!-- 상단: 이름 + D-N 뱃지 -->
        <div class="gps-card__top">
            <span class="gps-card__name">{{ challenge.name }}</span>
            <span class="gps-card__badge">시작 D-{{ challenge.daysUntilStart }}</span>
        </div>

        <!-- 중간: 평가타입 · 기간 · 금액 -->
        <p class="gps-card__desc">
            {{ challenge.evalType === 'DAILY' ? '일일결산' : '기간평가' }}
            · {{ challenge.totalDays }}일
            <template v-if="challenge.dailyLimit > 0">
                · 일 {{ challenge.dailyLimit.toLocaleString() }}원
            </template>
            <template v-else>
                · 무지출
            </template>
        </p>

        <!-- 하단: 멤버 + 참여코드/초대 -->
        <div class="gps-card__bottom">
            <div class="gps-card__members">
                <div class="gps-card__avatars">
                    <span
                        v-for="(m, i) in challenge.members"
                        :key="i"
                        class="gps-card__avatar"
                        :style="{
                            background: `var(${m.color})`,
                            marginLeft: i > 0 ? '-7px' : '0',
                            zIndex: challenge.members.length - i,
                        }"
                    >
                        {{ m.initial }}
                    </span>
                </div>
                <span class="gps-card__member-count">
                    {{ challenge.memberCount }} / {{ challenge.maxMembers }}명
                </span>
            </div>
            <span v-if="challenge.inviteCode" class="gps-card__code">
                참여코드 {{ challenge.inviteCode }}
            </span>
            <span v-else-if="challenge.isOwner" class="gps-card__invite">
                친구 초대하기
            </span>
        </div>
    </div>
</template>

<style scoped>
.gps-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
    padding: 14px 16px;
}

.gps-card__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gps-card__name {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gps-card__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
    background: var(--tt-bg-fill);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.gps-card__desc {
    margin: 8px 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    line-height: var(--tt-lh-normal);
}

.gps-card__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 12px;
}

.gps-card__members {
    display: flex;
    align-items: center;
    gap: 8px;
}

.gps-card__avatars {
    display: flex;
    align-items: center;
}

.gps-card__avatar {
    width: 26px;
    height: 26px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
    border: 2px solid var(--tt-bg);
    position: relative;
}

.gps-card__member-count {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.gps-card__code {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-blue);
}

.gps-card__invite {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-blue);
}
</style>
