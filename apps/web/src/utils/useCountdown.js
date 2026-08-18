import { ref, onMounted, onUnmounted } from 'vue';

/**
 * 실시간 카운트다운 composable.
 *
 * 각 아이템의 `deadline` 은 **절대시각**이다 (ISO 문자열 · Date · epoch ms).
 * 남은 분 수 같은 상대값을 받으면 화면을 열어 둔 채 몇 시간이 지났을 때 카운트다운이
 * 진입 시각 기준으로 굳는다. 절대시각이면 매초 다시 계산해도 값이 어긋나지 않는다.
 *
 * 서버는 오프셋 없는 `2026-08-16T15:00:00` 을 준다. 이 형태를 `new Date()` 에 넣으면
 * 브라우저가 **로컬 시각**으로 읽는다 — 서버도 브라우저도 KST 라 그대로 맞다.
 *
 * @param {import('vue').Ref<Array>} itemsRef  — `{ id, deadline }` 을 가진 배열 ref
 * @returns {{ countdowns }} ref<{ [id]: { text: string, urgent: boolean } }>
 */
export function useCountdown(itemsRef) {
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
            /* 마감을 못 읽으면 키를 비운다. 호출부가 '--:--:--' 로 대신 그린다. */
            const deadlineMs = new Date(item.deadline).getTime();
            if (!Number.isNaN(deadlineMs)) {
                map[item.id] = format(deadlineMs);
            }
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

    return { countdowns };
}
