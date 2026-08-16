<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { toPng } from 'html-to-image';
import { useRoute, useRouter } from 'vue-router';
import ChallengePageHeader from '@/components/challenge/ChallengePageHeader.vue';
import PersonalCertificateRatioSelector from '@/components/challenge/personal/ranking/PersonalCertificateRatioSelector.vue';
import PersonalCertificateShareSheet from '@/components/challenge/personal/ranking/PersonalCertificateShareSheet.vue';
import PersonalCertificateTitleSelector from '@/components/challenge/personal/ranking/PersonalCertificateTitleSelector.vue';
import PersonalHonorCertificate from '@/components/challenge/personal/ranking/PersonalHonorCertificate.vue';
import { fetchMissionCertificate } from '@/api/personalMission';

const route = useRoute();
const router = useRouter();
const certificateElement = ref(null);
const isSaving = ref(false);
const saveError = ref('');
const isShareOpen = ref(false);
const isPreparingShare = ref(false);
const shareFile = ref(null);
const shareMessage = ref('');
const certificate = ref(null);
const isLoading = ref(true);
const loadError = ref('');

const period = computed(() => {
    const queryPeriod = typeof route.query.month === 'string' ? route.query.month : '';
    return /^\d{4}-(0[1-9]|1[0-2])$/.test(queryPeriod) ? queryPeriod : '';
});

const ranking = computed(() => certificate.value);
const periodLabel = computed(() => period.value.replace('-', '.'));
const certificateTitles = computed(() => {
    if (!ranking.value) {
        return [];
    }
    return [
        `상위 ${ranking.value.myRanking.topPercent}%의 판결력`,
        '이번 달 진짜 무죄',
        '절약 판결 갱신',
    ];
});
const selectedTitle = ref('');
const selectedRatio = ref('portrait');

watch(period, () => {
    selectedTitle.value = '';
});

const judgmentDate = computed(() => {
    const [year, month] = period.value.split('-').map(Number);
    const lastDay = new Date(year, month, 0).getDate();
    return `${year}.${String(month).padStart(2, '0')}.${String(lastDay).padStart(2, '0')}`;
});

function goBack() {
    router.push({
        name: 'personalRanking',
        query: { month: period.value },
    });
}

async function loadCertificate() {
    if (!period.value) {
        loadError.value = '조회할 인증서 월이 올바르지 않아요.';
        isLoading.value = false;
        return;
    }

    isLoading.value = true;
    loadError.value = '';
    try {
        certificate.value = await fetchMissionCertificate(period.value);
        selectedTitle.value = `상위 ${certificate.value.myRanking.topPercent}%의 판결력`;
    } catch (error) {
        loadError.value = error.message || '인증서 데이터를 불러오지 못했어요.';
    } finally {
        isLoading.value = false;
    }
}

onMounted(loadCertificate);

async function createCertificateImage() {
    await nextTick();
    await document.fonts?.ready;

    const backgroundColor = getComputedStyle(document.documentElement)
        .getPropertyValue('--tt-bg')
        .trim();

    return toPng(certificateElement.value, {
        backgroundColor,
        cacheBust: true,
        pixelRatio: 2,
    });
}

async function saveCertificateImage(imageUrl = '') {
    if (!certificateElement.value || isSaving.value) {
        return;
    }

    isSaving.value = true;
    saveError.value = '';

    try {
        const certificateImageUrl = imageUrl || (await createCertificateImage());
        const downloadLink = document.createElement('a');
        downloadLink.download = `tangtang-honor-certificate-${period.value}.png`;
        downloadLink.href = certificateImageUrl;
        downloadLink.click();
    } catch (error) {
        console.error('명예 인증서 이미지 저장에 실패했습니다.', error);
        saveError.value = '이미지를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.';
    } finally {
        isSaving.value = false;
    }
}

async function openShareSheet() {
    isShareOpen.value = true;
    isPreparingShare.value = true;
    shareFile.value = null;
    shareMessage.value = '공유 이미지를 준비하고 있어요.';

    try {
        const imageUrl = await createCertificateImage();
        const imageBlob = await fetch(imageUrl).then((response) => response.blob());
        shareFile.value = new File([imageBlob], `tangtang-honor-certificate-${period.value}.png`, {
            type: 'image/png',
        });
        shareMessage.value = '공유할 이미지를 준비했어요.';
    } catch (error) {
        console.error('공유 이미지 준비에 실패했습니다.', error);
        shareMessage.value = '공유 이미지를 준비하지 못했어요.';
    } finally {
        isPreparingShare.value = false;
    }
}

async function shareCertificate() {
    if (!shareFile.value) {
        return;
    }

    try {
        const shareData = {
            title: selectedTitle.value,
            text: '탕탕 개인 미션 명예 인증서를 확인해 보세요.',
            files: [shareFile.value],
        };

        if (navigator.share && navigator.canShare?.({ files: shareData.files })) {
            await navigator.share(shareData);
            shareMessage.value = '공유를 완료했어요.';
            return;
        }

        if (navigator.share) {
            await navigator.share({
                title: shareData.title,
                text: shareData.text,
                url: window.location.href,
            });
            return;
        }

        await copyCertificateLink();
    } catch (error) {
        if (error.name !== 'AbortError') {
            console.error('명예 인증서 공유에 실패했습니다.', error);
            shareMessage.value = '공유하지 못했어요. 다시 시도해 주세요.';
        }
    }
}

async function copyCertificateLink() {
    try {
        await navigator.clipboard.writeText(window.location.href);
        shareMessage.value = '인증서 링크를 복사했어요.';
    } catch (error) {
        console.error('명예 인증서 링크 복사에 실패했습니다.', error);
        shareMessage.value = '링크를 복사하지 못했어요.';
    }
}
</script>

<template>
    <main class="personal-certificate">
        <ChallengePageHeader
            class="personal-certificate__page-header"
            title="명예 인증서"
            back-label="개인 미션 랭킹으로 돌아가기"
            @back="goBack"
        />

        <p v-if="isLoading" class="personal-certificate__notice" role="status">
            인증서 데이터를 불러오고 있어요.
        </p>

        <p v-else-if="loadError" class="personal-certificate__notice" role="alert">
            {{ loadError }}
        </p>

        <template v-else-if="ranking">
            <div ref="certificateElement">
                <PersonalHonorCertificate
                    :ranking="ranking"
                    :period-label="periodLabel"
                    :judgment-date="judgmentDate"
                    :title="selectedTitle"
                    :ratio="selectedRatio"
                />
            </div>

            <PersonalCertificateTitleSelector v-model="selectedTitle" :titles="certificateTitles" />
            <PersonalCertificateRatioSelector v-model="selectedRatio" />

            <div class="personal-certificate__actions">
                <button type="button" :disabled="isSaving" @click="saveCertificateImage()">
                    {{ isSaving ? '이미지 만드는 중...' : '이미지 저장' }}
                </button>
                <button type="button" @click="openShareSheet">공유하기</button>
            </div>

            <p v-if="saveError" class="personal-certificate__notice" role="alert">
                {{ saveError }}
            </p>

            <PersonalCertificateShareSheet
                v-model="isShareOpen"
                :certificate-title="selectedTitle"
                :is-preparing="isPreparingShare"
                :message="shareMessage"
                @share="shareCertificate"
                @copy-link="copyCertificateLink"
            />
        </template>
    </main>
</template>

<style scoped src="./PersonalCertificateView.css"></style>
