/*
 * 개인 미션 챌린지 도메인의 라우트
 * 사용자에게 보이는 메뉴명은 '재판'이지만 코드 이름은 personalMissionChallenge로 통일
 */
const personalMissionChallengeRoutes = [
    {
        path: '/mission/personal',
        name: 'personalMissionChallenge',
        component: () => import('@/views/challenge/personal/PersonalMissionHomeView.vue'),
        meta: { title: '개인 미션 챌린지' },
    },
    {
        path: '/mission/personal/difficulty',
        name: 'personalMissionChallengeDifficulty',
        component: () => import('@/views/challenge/personal/PersonalMissionDifficultyView.vue'),
        meta: { title: '개인 미션 난이도 설정' },
    },
    {
        path: '/mission/personal/ranking',
        name: 'personalRanking',
        component: () => import('@/views/challenge/personal/PersonalRankingView.vue'),
        meta: { title: '개인 미션 랭킹' },
    },
    {
        path: '/mission/personal/ranking/certificate',
        name: 'personalCertificate',
        component: () => import('@/views/challenge/personal/PersonalCertificateView.vue'),
        meta: { title: '개인 미션 명예 인증서' },
    },
    {
        path: '/group-challenges',
        name: 'groupChallenge',
        component: () => import('@/views/challenge/group/GroupChallengeHomeView.vue'),
        meta: { title: '그룹 챌린지' },
    },
];

export default personalMissionChallengeRoutes;
