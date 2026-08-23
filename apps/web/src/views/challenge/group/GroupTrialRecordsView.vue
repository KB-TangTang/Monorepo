<!--
  재판 기록 — 확정된 재판(GUILTY · INNOCENT) 목록

  **라우트 두 개가 이 뷰 하나를 공유한다.**
  - `/group-challenges/trial-records`      → 내가 속한 모든 그룹의 기록 (지방법원 홈 진입)
  - `/group-challenges/:id/trial-records`  → 한 그룹의 기록 (그룹 상세 진입)

  화면이 완전히 같고 데이터 출처만 다르다. 뷰를 둘로 쪼개면 요약 스트립·빈 상태·행 클릭
  라우팅이 두 벌이 되고, 둘 중 하나만 고쳐지는 날이 온다. `route.params.id` 유무로 가른다.

  확정된 재판은 여태 앱 안에 볼 곳이 없었다. 목록 API 자체가 없었고(`findClosedTrialStats` 의
  집계 숫자 3개가 전부였다), 그룹 상세의 「재판 기록」 버튼은 명예 법정으로 가고 있었다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchAllMyTrialRecords, fetchGroupTrialRecords } from '@/api/groupChallenge';
import { toTrialRecordCard } from '@/utils/groupTrial';

import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import GroupTrialRecordCard from '@/components/challenge/group/GroupTrialRecordCard.vue';
import GroupHonorCourtEntry from '@/components/challenge/group/GroupHonorCourtEntry.vue';

const route = useRoute();
const router = useRouter();

/* 그룹 id 가 없으면 전체 모드다. 라우트가 갈리는 유일한 지점이라 여기서 한 번만 판단한다 */
const groupId = route.params.id ?? null;
const isAllMode = !groupId;

const records = ref([]);
const loading = ref(true);
const failed = ref(false);

onMounted(load);

async function load() {
    loading.value = true;
    failed.value = false;
    try {
        const list = isAllMode
            ? await fetchAllMyTrialRecords()
            : await fetchGroupTrialRecords(groupId);
        records.value = list.map(toTrialRecordCard);
    } catch {
        failed.value = true;
    } finally {
        loading.value = false;
    }
}

/*
 * 요약은 목록에서 센다. 그룹 상세 응답의 `trialStats` 는 **CLOSED 일 때만 채워져서**
 * (`ChallengeGroupDetailService.java:135`) 진행 중 그룹에서는 빈 값이 온다.
 * 이미 손에 목록이 있으니 요청을 더 보낼 이유가 없다.
 */
const guiltyCount = computed(
    () => records.value.filter((item) => item.verdict === 'GUILTY').length,
);
const innocentCount = computed(() => records.value.length - guiltyCount.value);

function openRecord({ item }) {
    /*
     * 목적지는 `toTrialRecordCard` 가 `verdictRouteName` 으로 이미 정해 놓았다 —
     * AI 판결은 동점 안내 화면, 나머지는 판결문이다. 여기서 다시 분기하면
     * 진입로마다 AI 판결을 모르는 곳이 생긴다.
     *
     * 확정 전이면 `null` 인데, 이 목록에는 확정 건만 오므로 정상적으로는 걸리지 않는다.
     * 옛 데이터에 `verdictMethod` 가 비어 있어도 화면이 죽지 않게 막아 둔다.
     */
    if (!item.routeName) return;
    router.push({
        name: item.routeName,
        params: { id: item.groupId, indictmentId: item.id },
    });
}

function goToRanking() {
    router.push({ name: 'groupChallengeRanking', params: { id: groupId } });
}
</script>

<template>
    <div class="trial-records">
        <BaseBackHeader title="재판 기록" />

        <StateLoading v-if="loading" message="기록을 불러오는 중이에요" />

        <StateError
            v-else-if="failed"
            title="기록을 불러오지 못했어요"
            message="잠시 후 다시 시도해 주세요."
            @retry="load"
        />

        <StateEmpty
            v-else-if="!records.length"
            title="아직 확정된 재판이 없어요"
            description="재판이 끝나면 판결과 개표 결과가 여기에 남아요."
        />

        <template v-else>
            <!-- 요약 스트립 — 목록을 훑기 전에 규모를 먼저 준다 -->
            <div class="trial-records__summary">
                <span class="trial-records__total">총 {{ records.length }}건</span>
                <span class="trial-records__split">
                    <span class="trial-records__guilty">유죄 {{ guiltyCount }}</span>
                    <span class="trial-records__dot">·</span>
                    <span class="trial-records__innocent">무죄 {{ innocentCount }}</span>
                </span>
            </div>

            <GroupTrialRecordCard :items="records" :show-group="isAllMode" @open="openRecord" />
        </template>

        <!--
             명예 법정은 그룹 모드에서만 붙인다. 전체 기록에는 그룹이 여럿이라
             어느 그룹의 순위로 보내야 할지 정할 수 없다.
             빈 상태·실패에도 남긴다 — 기록이 0건이어도 순위는 있다.
        -->
        <GroupHonorCourtEntry
            v-if="!isAllMode && !loading"
            class="trial-records__honor"
            variant="active"
            @open="goToRanking"
        />
    </div>
</template>

<style scoped>
.trial-records {
    min-height: 100vh;
    padding: var(--tt-space-4) var(--tt-space-4) var(--tt-space-10);
    background: var(--tt-bg-page);
}

.trial-records__summary {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-2);
    padding: 0 4px var(--tt-space-3);
}
.trial-records__total {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}
.trial-records__split {
    display: flex;
    align-items: baseline;
    gap: 5px;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
}
.trial-records__guilty {
    color: var(--tt-danger);
}
.trial-records__innocent {
    color: var(--tt-success);
}
.trial-records__dot {
    color: var(--tt-text-hint);
}

.trial-records__honor {
    margin-top: var(--tt-space-5);
}
</style>
