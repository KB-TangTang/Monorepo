/*
 * 개인 미션 챌린지 도메인의 라우트
 * 사용자에게 보이는 메뉴명은 '재판'이지만 코드 이름은 personalMissionChallenge로 통일
 */
const personalMissionChallengeRoutes = [
    {
        path: '/personal-missions',
        name: 'personalMissionChallenge',
        component: () => import('@/views/challenge/personal/PersonalMissionHomeView.vue'),
        meta: { title: '개인 미션 챌린지' },
    },
    {
        path: '/personal-missions/difficulty',
        name: 'personalMissionChallengeDifficulty',
        component: () => import('@/views/challenge/personal/PersonalMissionDifficultyView.vue'),
        meta: { title: '개인 미션 난이도 설정' },
    },
];

export default personalMissionChallengeRoutes;
