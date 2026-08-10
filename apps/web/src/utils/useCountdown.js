import { ref, onMounted, onUnmounted } from 'vue';

/**
 * 실시간 카운트다운 composable.
 *
 * @param {import('vue').Ref<Array>} itemsRef  — 각 아이템에 deadlineMinutes 가 있는 배열 ref
 * @returns {{ countdowns, baseTime }}
 *   countdowns: ref<{ [id]: { text: string, urgent: boolean } }>
 *   baseTime: 페이지 진입 시각 (ms). 아이템 마감 = baseTime + deadlineMinutes * 60000
 */
export function useCountdown(itemsRef) {
    const baseTime = Date.now();
    const countdowns = ref({});
    let timer = null;

    function pad(n) {
        return String(n).padStart(2, '0');
    }

    function format(deadlineMs) {
        const left = Math.max(0, deadlineMs - Date.now());

        /* 24시간 이상이면 D-n */
        if (left >= 86400000) {
            return { text: 'D-' + Math.ceil(left / 86400000), urgent: false };
        }

        const h = Math.floor(left / 3600000);
        const m = Math.floor(left / 60000) % 60;
        const s = Math.floor(left / 1000) % 60;

        /* 6시간 미만이면 긴급 */
        return { text: `${pad(h)}:${pad(m)}:${pad(s)}`, urgent: left < 21600000 };
    }

    function tick() {
        const map = {};
        for (const item of itemsRef.value) {
            const deadlineMs = baseTime + item.deadlineMinutes * 60000;
            map[item.id] = format(deadlineMs);
        }
        countdowns.value = map;
    }

    onMounted(() => {
        tick();
        timer = setInterval(tick, 1000);
    });

    onUnmounted(() => {
        clearInterval(timer);
    });

    return { countdowns, baseTime };
}
