import { defineStore } from 'pinia';

/**
 * TEMP(#241): 챌린지 리포트 백엔드 상세 연동과 검증이 끝나면
 * TempChallengeReportSourceToggle 및 이 임시 데이터 소스 상태를 함께 삭제한다.
 */
export const useChallengeReportStore = defineStore('challengeReport', {
    state: () => ({
        reportSource: 'mock',
    }),
    actions: {
        setReportSource(source) {
            if (['api', 'mock'].includes(source)) {
                this.reportSource = source;
            }
        },
    },
});
