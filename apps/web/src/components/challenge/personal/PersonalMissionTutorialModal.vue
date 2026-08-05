<!--
  용도: 개인 미션 챌린지 참여 동의 직후 한 번 보여주는 3단계 튜토리얼.
  순서: 오늘의 미션 → 난이도 선택 → 명예법정.
-->
<script setup>
import { computed, ref, watch } from 'vue';
import BaseModal from '@/components/common/BaseModal.vue';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
});

const emit = defineEmits(['update:modelValue', 'complete', 'skip']);

const currentStep = ref(0);
const totalSteps = 3;
const isLastStep = computed(() => currentStep.value === totalSteps - 1);

watch(
    () => props.modelValue,
    (isOpen) => {
        if (isOpen) {
            currentStep.value = 0;
        }
    },
);

function close() {
    emit('update:modelValue', false);
}

function skip() {
    close();
    emit('skip');
}

function next() {
    if (!isLastStep.value) {
        currentStep.value += 1;
        return;
    }

    close();
    emit('complete');
}
</script>

<template>
    <BaseModal
        :model-value="modelValue"
        :close-on-overlay="false"
        :close-on-esc="false"
        :show-close="false"
        title="개인 미션 챌린지 튜토리얼"
        @update:model-value="emit('update:modelValue', $event)"
    >
        <div class="tutorial">
            <img
                class="tutorial__judge"
                src="@/assets/images/emotions/48_judging.png"
                alt="개인 미션을 안내하는 탕이"
            />

            <span class="tutorial__step">{{ currentStep + 1 }}/{{ totalSteps }}</span>

            <!-- 1단계: 오늘의 미션 -->
            <template v-if="currentStep === 0">
                <section class="tutorial__visual tutorial__visual--mission">
                    <p class="tutorial__kicker">TODAY'S MISSION</p>
                    <div class="tutorial__mission-card">
                        <span>오늘의 미션</span>
                        <strong>카페 5,500원 안에서</strong>
                        <div aria-hidden="true"><i></i></div>
                    </div>
                </section>

                <h2>매일 미션이 하나씩 도착해요</h2>
                <p class="tutorial__description">
                    내 최근 소비를 기준으로 오늘 지킬 금액이 정해져요. 자정에 새 미션이 열려요.
                </p>
            </template>

            <!-- 2단계: 난이도 -->
            <template v-else-if="currentStep === 1">
                <section class="tutorial__visual tutorial__visual--difficulty">
                    <p class="tutorial__kicker">DIFFICULTY</p>
                    <div class="tutorial__difficulty-list">
                        <div class="tutorial__difficulty tutorial__difficulty--active">
                            <strong>하</strong>
                            <span>+10점</span>
                        </div>
                        <div class="tutorial__difficulty">
                            <strong>중</strong>
                            <span>+20점</span>
                        </div>
                        <div class="tutorial__difficulty">
                            <strong>상</strong>
                            <span>+35점</span>
                        </div>
                    </div>
                </section>

                <h2>난이도는 내가 골라요</h2>
                <p class="tutorial__description">
                    하·중·상 중에서 고르면 목표 금액이 달라져요. 처음엔 ‘하’로 시작해도 좋아요.
                </p>
            </template>

            <!-- 3단계: 명예법정 -->
            <template v-else>
                <section class="tutorial__visual tutorial__visual--honor">
                    <p class="tutorial__kicker">HALL OF HONOR</p>
                    <div class="tutorial__podium" aria-hidden="true">
                        <span class="tutorial__podium-second">2</span>
                        <span class="tutorial__podium-first">1</span>
                        <span class="tutorial__podium-third">3</span>
                    </div>
                </section>

                <h2>성공하면 명예 법정에 올라요</h2>
                <p class="tutorial__description">
                    연속 성공일이 쌓이면 점수가 더 붙어요. 점수는 매월 1일 0점부터 다시 시작해요.
                </p>
            </template>

            <div class="tutorial__actions">
                <button type="button" class="tutorial__skip" @click="skip">건너뛰기</button>
                <button type="button" class="tutorial__next" @click="next">
                    {{ isLastStep ? '시작하기' : '다음' }}
                </button>
            </div>

            <div class="tutorial__indicators" aria-label="튜토리얼 진행 상태">
                <span
                    v-for="step in totalSteps"
                    :key="step"
                    :class="{ 'tutorial__indicator--active': currentStep === step - 1 }"
                ></span>
            </div>
        </div>
    </BaseModal>
</template>

<!--
  BaseModal은 Teleport로 body 아래에 렌더링되므로 부모의 scoped 선택자가 패널까지 도달하지 않는다.
  CSS에서는 :has(.tutorial)로 이 튜토리얼을 담은 모달만 한정해 꾸민다.
-->
<style src="./PersonalMissionTutorialModal.css"></style>
