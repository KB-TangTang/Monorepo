<!--
  용도: 투자증권 상세 화면의 보유 종목 한 행. 종목명 · 보유 수량 · 평가금액 · 수익률·증감액을 보여주고,
  클릭하면 바로 아래로 평가금액 · 매입금액 · 평가손익 · 수익률 · 보유수량 · 매입평균 상세가 펼쳐진다.
  언제 쓰는지: AssetInvestmentView 하나뿐. returnRate 는 소수(0.124 = +12.4%)로 받는다.
  펼침 상태는 이 컴포넌트가 소유하지 않는다 — 부모가 expanded prop 으로 내려주고 toggle 이벤트로 반응한다
  (여러 종목 중 한 번에 하나만 펼쳐지도록 부모가 단일 확장을 보장하기 위함).
-->
<script setup>
import { computed } from 'vue';
import { ChevronDownIcon } from '@heroicons/vue/24/solid';
import {
    formatWon,
    formatSignedWon,
    formatSignedPercent,
    toneColor,
    getHoldingCost,
    getHoldingAveragePrice,
} from '@/utils/asset';

const props = defineProps({
    badge: { type: String, required: true },
    name: { type: String, required: true },
    quantity: { type: Number, required: true },
    amount: { type: Number, required: true },
    gainAmount: { type: Number, required: true },
    returnRate: { type: Number, required: true },
    tone: { type: String, default: 'gray' },
    expanded: { type: Boolean, default: false },
});

defineEmits(['toggle']);

const cost = computed(() => getHoldingCost(props.amount, props.gainAmount));
const averagePrice = computed(() => getHoldingAveragePrice(cost.value, props.quantity));
const rateTone = computed(() =>
    props.returnRate < 0 ? 'holding-row__rate--down' : 'holding-row__rate--up',
);
</script>

<template>
    <li class="holding-row">
        <button
            type="button"
            class="holding-row__trigger"
            :aria-expanded="expanded"
            @click="$emit('toggle')"
        >
            <span
                class="holding-row__avatar"
                :style="{ background: toneColor(tone) }"
                aria-hidden="true"
            >
                {{ badge }}
            </span>
            <div class="holding-row__info">
                <p class="holding-row__name">{{ name }}</p>
                <p class="holding-row__meta">{{ quantity }}주</p>
            </div>
            <div class="holding-row__figures">
                <p class="holding-row__amount">{{ formatWon(amount) }}</p>
                <p class="holding-row__rate" :class="rateTone">
                    {{ formatSignedPercent(returnRate) }} ({{ formatSignedWon(gainAmount) }})
                </p>
            </div>
            <ChevronDownIcon
                class="holding-row__chevron"
                :class="{ 'holding-row__chevron--open': expanded }"
                aria-hidden="true"
            />
        </button>

        <div class="holding-row__detail-wrap" :class="{ 'holding-row__detail-wrap--open': expanded }">
            <div class="holding-row__detail">
                <div class="holding-row__detail-row">
                    <span class="holding-row__detail-label">평가금액</span>
                    <span class="holding-row__detail-value">{{ formatWon(amount) }}</span>
                </div>
                <div class="holding-row__detail-row">
                    <span class="holding-row__detail-label">매입금액</span>
                    <span class="holding-row__detail-value">{{ formatWon(cost) }}</span>
                </div>
                <div class="holding-row__detail-row">
                    <span class="holding-row__detail-label">평가손익</span>
                    <span class="holding-row__detail-value" :class="rateTone">
                        {{ formatSignedWon(gainAmount) }}
                    </span>
                </div>
                <div class="holding-row__detail-row">
                    <span class="holding-row__detail-label">수익률</span>
                    <span class="holding-row__detail-value" :class="rateTone">
                        {{ formatSignedPercent(returnRate) }}
                    </span>
                </div>
                <div class="holding-row__detail-row">
                    <span class="holding-row__detail-label">보유수량</span>
                    <span class="holding-row__detail-value">{{ quantity }}주</span>
                </div>
                <div class="holding-row__detail-row">
                    <span class="holding-row__detail-label">매입평균</span>
                    <span class="holding-row__detail-value">{{ formatWon(averagePrice) }}</span>
                </div>
            </div>
        </div>
    </li>
</template>

<style scoped>
.holding-row {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    overflow: hidden;
}

.holding-row__detail-wrap {
    display: grid;
    grid-template-rows: 0fr;
    transition: grid-template-rows 0.25s ease;
}

.holding-row__detail-wrap--open {
    grid-template-rows: 1fr;
}

.holding-row__trigger {
    display: flex;
    width: 100%;
    align-items: center;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4);
    font: inherit;
    color: inherit;
    text-align: left;
    background: none;
    border: none;
    cursor: pointer;
}

.holding-row__avatar {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-inverse);
    border-radius: var(--tt-radius-full);
}

.holding-row__info {
    flex: 1;
    min-width: 0;
}

.holding-row__name {
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.holding-row__meta {
    margin-top: 2px;
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.holding-row__figures {
    flex-shrink: 0;
    text-align: right;
}

.holding-row__amount {
    font-family: var(--tt-font-mono);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.holding-row__rate {
    margin-top: 2px;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
}

.holding-row__rate--up {
    color: var(--tt-success);
}

.holding-row__rate--down {
    color: var(--tt-danger);
}

.holding-row__chevron {
    flex-shrink: 0;
    width: 18px;
    height: 18px;
    color: var(--tt-text-hint);
    transition: transform 0.25s ease;
}

.holding-row__chevron--open {
    transform: rotate(180deg);
}

.holding-row__detail {
    min-height: 0;
    overflow: hidden;
    padding: 0 var(--tt-space-4) var(--tt-space-4);
}

.holding-row__detail-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--tt-space-2) 0;
    border-top: 1px solid var(--tt-border-light);
}

.holding-row__detail-label {
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.holding-row__detail-value {
    font-family: var(--tt-font-mono);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

</style>
