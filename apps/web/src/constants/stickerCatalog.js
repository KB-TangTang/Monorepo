/**
 * 탕이 스티커 카탈로그
 * - 판사 탕이 (judgment/) : 3종
 * - 검사 탕이 (prosecutor_tangtang/) : 9종
 * - 표정 (emotions/) : 57종
 *
 * STICKER_MAP[stickerId] → { src, label }
 * getStickersByCategory(categoryKey) → [{ id, src, label }, …]
 */

/* ── 판사 탕이 ──────────────────────────────────────────── */
import judgeDefault from '@/assets/images/judgment/judge_tangtang_default.png';
import judgeGavelRaised from '@/assets/images/judgment/judge_tangtang_gavel_raised.png';
import judgeGavelStrike from '@/assets/images/judgment/judge_tangtang_gavel_strike.png';

/* ── 검사 탕이 ──────────────────────────────────────────── */
import prosecutorA from '@/assets/images/prosecutor_tangtang/prosecutor_tangtang_A.png';
import prosecutorMomentRaidV1 from '@/assets/images/prosecutor_tangtang/consumer_moment_raid_v1_2d_no_emphasis.png';
import prosecutorMomentRaidV2 from '@/assets/images/prosecutor_tangtang/consumer_moment_raid_v2_2d_no_emphasis.png';
import prosecutorDirectStare from '@/assets/images/prosecutor_tangtang/direct_stare_close_uncomfortable.png';
import prosecutorFieldV1 from '@/assets/images/prosecutor_tangtang/field_verification_v1_one_leg_no_emphasis.png';
import prosecutorFieldV2 from '@/assets/images/prosecutor_tangtang/field_verification_v2.png';
import prosecutorMagnifying from '@/assets/images/prosecutor_tangtang/magnifying_stare.png';
import prosecutorSceneRaidV1 from '@/assets/images/prosecutor_tangtang/scene_raid_v1_one_leg_no_emphasis.png';
import prosecutorSceneRaidV2 from '@/assets/images/prosecutor_tangtang/scene_raid_v2_one_leg_no_emphasis.png';

/* ── 표정 ───────────────────────────────────────────────── */
import e01 from '@/assets/images/emotions/01_happy.png';
import e02 from '@/assets/images/emotions/02_big_laugh.png';
import e03 from '@/assets/images/emotions/03_gentle_smile.png';
import e04 from '@/assets/images/emotions/04_proud.png';
import e05 from '@/assets/images/emotions/05_excited.png';
import e06 from '@/assets/images/emotions/06_love.png';
import e07 from '@/assets/images/emotions/07_heart_eyes.png';
import e08 from '@/assets/images/emotions/08_grateful.png';
import e09 from '@/assets/images/emotions/09_cheering.png';
import e10 from '@/assets/images/emotions/10_celebration.png';
import e11 from '@/assets/images/emotions/11_sad.png';
import e12 from '@/assets/images/emotions/12_crying.png';
import e13 from '@/assets/images/emotions/13_sobbing.png';
import e14 from '@/assets/images/emotions/14_disappointed.png';
import e15 from '@/assets/images/emotions/15_lonely.png';
import e16 from '@/assets/images/emotions/16_worried.png';
import e17 from '@/assets/images/emotions/17_anxious.png';
import e18 from '@/assets/images/emotions/18_scared.png';
import e19 from '@/assets/images/emotions/19_shocked.png';
import e20 from '@/assets/images/emotions/20_confused.png';
import e21 from '@/assets/images/emotions/21_angry.png';
import e22 from '@/assets/images/emotions/22_furious.png';
import e23 from '@/assets/images/emotions/23_annoyed.png';
import e24 from '@/assets/images/emotions/24_sulking.png';
import e25 from '@/assets/images/emotions/25_skeptical.png';
import e26 from '@/assets/images/emotions/26_suspicious.png';
import e27 from '@/assets/images/emotions/27_embarrassed.png';
import e28 from '@/assets/images/emotions/28_shy.png';
import e29 from '@/assets/images/emotions/29_flustered.png';
import e30 from '@/assets/images/emotions/30_awkward.png';
import e31 from '@/assets/images/emotions/31_sleepy.png';
import e32 from '@/assets/images/emotions/32_sleeping.png';
import e33 from '@/assets/images/emotions/33_tired.png';
import e34 from '@/assets/images/emotions/34_exhausted.png';
import e35 from '@/assets/images/emotions/35_sick.png';
import e36 from '@/assets/images/emotions/36_dizzy.png';
import e37 from '@/assets/images/emotions/37_hungry.png';
import e38 from '@/assets/images/emotions/38_full.png';
import e39 from '@/assets/images/emotions/39_cold.png';
import e40 from '@/assets/images/emotions/40_hot.png';
import e41 from '@/assets/images/emotions/41_okay.png';
import e42 from '@/assets/images/emotions/42_thumbs_up.png';
import e43 from '@/assets/images/emotions/43_apology.png';
import e44 from '@/assets/images/emotions/44_please.png';
import e45 from '@/assets/images/emotions/45_no.png';
import e46 from '@/assets/images/emotions/46_stop.png';
import e47 from '@/assets/images/emotions/47_thinking.png';
import e48 from '@/assets/images/emotions/48_judging.png';
import e49 from '@/assets/images/emotions/49_verdict.png';
import e50 from '@/assets/images/emotions/50_goodbye.png';
import e51 from '@/assets/images/emotions/51_thumbs_down.png';
import e52 from '@/assets/images/emotions/52_judge_thumbs_down.png';
import e53 from '@/assets/images/emotions/53_with_trophy_ver1.png';
import e54 from '@/assets/images/emotions/54_with_trophy_ver2.png';
import e55 from '@/assets/images/emotions/55_with_trophy_ver3.png';
import e56 from '@/assets/images/emotions/56_with_trophy_ver4.png';
import e57 from '@/assets/images/emotions/57_chat.png';

/* ── 스티커 맵 ──────────────────────────────────────────── */
export const STICKER_MAP = {
    /* 판사 */
    judge_default:      { src: judgeDefault,      label: '판사 탕이' },
    judge_gavel_raised: { src: judgeGavelRaised,  label: '망치 들기' },
    judge_gavel_strike: { src: judgeGavelStrike,  label: '망치 내리기' },

    /* 검사 */
    prosecutor_a:               { src: prosecutorA,             label: '검사 탕이' },
    prosecutor_moment_raid_v1:  { src: prosecutorMomentRaidV1,  label: '현행범 체포 1' },
    prosecutor_moment_raid_v2:  { src: prosecutorMomentRaidV2,  label: '현행범 체포 2' },
    prosecutor_direct_stare:    { src: prosecutorDirectStare,   label: '직접 응시' },
    prosecutor_field_v1:        { src: prosecutorFieldV1,       label: '현장검증 1' },
    prosecutor_field_v2:        { src: prosecutorFieldV2,       label: '현장검증 2' },
    prosecutor_magnifying:      { src: prosecutorMagnifying,    label: '돋보기' },
    prosecutor_scene_raid_v1:   { src: prosecutorSceneRaidV1,   label: '현장급습 1' },
    prosecutor_scene_raid_v2:   { src: prosecutorSceneRaidV2,   label: '현장급습 2' },

    /* 표정 */
    emotion_happy:        { src: e01, label: '행복' },
    emotion_big_laugh:    { src: e02, label: '빵터짐' },
    emotion_gentle_smile: { src: e03, label: '미소' },
    emotion_proud:        { src: e04, label: '뿌듯' },
    emotion_excited:      { src: e05, label: '신남' },
    emotion_love:         { src: e06, label: '사랑' },
    emotion_heart_eyes:   { src: e07, label: '하트눈' },
    emotion_grateful:     { src: e08, label: '감사' },
    emotion_cheering:     { src: e09, label: '응원' },
    emotion_celebration:  { src: e10, label: '축하' },
    emotion_sad:          { src: e11, label: '슬픔' },
    emotion_crying:       { src: e12, label: '눈물' },
    emotion_sobbing:      { src: e13, label: '오열' },
    emotion_disappointed: { src: e14, label: '실망' },
    emotion_lonely:       { src: e15, label: '외로움' },
    emotion_worried:      { src: e16, label: '걱정' },
    emotion_anxious:      { src: e17, label: '불안' },
    emotion_scared:       { src: e18, label: '무서움' },
    emotion_shocked:      { src: e19, label: '충격' },
    emotion_confused:     { src: e20, label: '혼란' },
    emotion_angry:        { src: e21, label: '화남' },
    emotion_furious:      { src: e22, label: '분노' },
    emotion_annoyed:      { src: e23, label: '짜증' },
    emotion_sulking:      { src: e24, label: '삐짐' },
    emotion_skeptical:    { src: e25, label: '의심' },
    emotion_suspicious:   { src: e26, label: '수상' },
    emotion_embarrassed:  { src: e27, label: '창피' },
    emotion_shy:          { src: e28, label: '수줍' },
    emotion_flustered:    { src: e29, label: '당황' },
    emotion_awkward:      { src: e30, label: '어색' },
    emotion_sleepy:       { src: e31, label: '졸림' },
    emotion_sleeping:     { src: e32, label: '수면' },
    emotion_tired:        { src: e33, label: '피곤' },
    emotion_exhausted:    { src: e34, label: '녹초' },
    emotion_sick:         { src: e35, label: '아픔' },
    emotion_dizzy:        { src: e36, label: '어지러움' },
    emotion_hungry:       { src: e37, label: '배고픔' },
    emotion_full:         { src: e38, label: '배부름' },
    emotion_cold:         { src: e39, label: '추위' },
    emotion_hot:          { src: e40, label: '더위' },
    emotion_okay:         { src: e41, label: '오케이' },
    emotion_thumbs_up:    { src: e42, label: '좋아요' },
    emotion_apology:      { src: e43, label: '미안' },
    emotion_please:       { src: e44, label: '부탁' },
    emotion_no:           { src: e45, label: '싫어' },
    emotion_stop:         { src: e46, label: '그만' },
    emotion_thinking:     { src: e47, label: '생각중' },
    emotion_judging:      { src: e48, label: '재판' },
    emotion_verdict:      { src: e49, label: '판결' },
    emotion_goodbye:      { src: e50, label: '안녕' },
    emotion_thumbs_down:  { src: e51, label: '싫어요' },
    emotion_judge_td:     { src: e52, label: '판사 싫어요' },
    emotion_trophy_v1:    { src: e53, label: '트로피 1' },
    emotion_trophy_v2:    { src: e54, label: '트로피 2' },
    emotion_trophy_v3:    { src: e55, label: '트로피 3' },
    emotion_trophy_v4:    { src: e56, label: '트로피 4' },
    emotion_chat:         { src: e57, label: '수다' },
};

/* ── 카테고리 ───────────────────────────────────────────── */
export const STICKER_CATEGORIES = [
    { key: 'emotion',     label: '표정',   prefix: 'emotion_' },
    { key: 'judge',       label: '판사',   prefix: 'judge_' },
    { key: 'prosecutor',  label: '검사',   prefix: 'prosecutor_' },
];

export function getStickersByCategory(categoryKey) {
    const cat = STICKER_CATEGORIES.find((c) => c.key === categoryKey);
    if (!cat) return [];
    return Object.entries(STICKER_MAP)
        .filter(([id]) => id.startsWith(cat.prefix))
        .map(([id, data]) => ({ id, ...data }));
}
