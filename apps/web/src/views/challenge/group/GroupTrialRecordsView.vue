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
import GroupRecordScopeSegment from '@/components/challenge/group/GroupRecordScopeSegment.vue';
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
 * 범위 필터. 서버에 다시 묻지 않고 손에 있는 목록을 가른다 —
 * 기록은 확정된 것만 와서 화면에 있는 동안 늘지 않는다.
 */
const scope = ref('all');

const mineRecords = computed(() => records.value.filter((item) => item.isMine));
const visibleRecords = computed(() => {
    if (scope.value === 'mine') return mineRecords.value;
    if (scope.value === 'others') return records.value.filter((item) => !item.isMine);
    return records.value;
});

const scopeCounts = computed(() => ({
    all: records.value.length,
    mine: mineRecords.value.length,
    others: records.value.length - mineRecords.value.length,
}));

/*
 * 요약은 목록에서 센다. 그룹 상세 응답의 `trialStats` 는 **CLOSED 일 때만 채워져서**
 * (`ChallengeGroupDetailService.java:135`) 진행 중 그룹에서는 빈 값이 온다.
 * 이미 손에 목록이 있으니 요청을 더 보낼 이유가 없다.
 *
 * **보이는 것만 센다.** 전체 건수를 그대로 두면 「내 재판」 탭에서 목록엔 1건인데
 * 위에는 총 8건이라 적혀 요약이 목록과 다른 말을 한다.
 */
const guiltyCount = computed(
    () => visibleRecords.value.filter((item) => item.verdict === 'GUILTY').length,
);
const innocentCount = computed(() => visibleRecords.value.length - guiltyCount.value);

/** 탭을 좁혀서 0건이 된 경우의 문구. 기록이 아예 없는 경우(`StateEmpty`)와 다른 상황이다 */
const emptyScopeTitle = computed(() =>
    scope.value === 'mine' ? '내 재판 기록이 없어요' : '그룹원의 재판 기록이 없어요',
);

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
            <!--
                 범위 세그먼트. 기록이 적어도 접지 않는다 — 건수에 따라 있다 없다 하면
                 들어올 때마다 화면 구조가 달라져 어디를 눌러야 할지 매번 다시 찾는다.
            -->
            <GroupRecordScopeSegment
                v-model="scope"
                :counts="scopeCounts"
                class="trial-records__scope"
            />

            <!-- 요약 스트립 — 목록을 훑기 전에 규모를 먼저 준다. 세그먼트를 따라 같이 좁혀진다 -->
            <div class="trial-records__summary">
                <span class="trial-records__counts">
                    <span class="trial-records__total">총 {{ visibleRecords.length }}건</span>
                    <span class="trial-records__split">
                        <span class="trial-records__guilty">유죄 {{ guiltyCount }}</span>
                        <span class="trial-records__dot">·</span>
                        <span class="trial-records__innocent">무죄 {{ innocentCount }}</span>
                    </span>
                </span>

                <!--
                     정렬 축을 적어 둔다. 행에서 결산일을 뺀 뒤로 화면에 시각이 하나도 안 남아
                     순서를 눈으로 확인할 방법이 없어졌다.

                     **고를 수 있는 것처럼 보이면 안 된다.** 정렬은 SQL 이 끝내 놓았고
                     (`IndictmentMapper.xml:312` · `:328` 의 `ORDER BY i.updated_at DESC`)
                     바꿀 수단이 없다. 버튼 모양을 주면 눌러도 아무 일이 없는 자리가 된다.
                -->
                <span class="trial-records__sort">최근 확정순</span>
            </div>

            <StateEmpty
                v-if="!visibleRecords.length"
                :title="emptyScopeTitle"
                description="다른 범위를 눌러 보세요."
            />
            <GroupTrialRecordCard v-else :items="visibleRecords" @open="openRecord" />
        </template>

        <!--
             명예 법정은 그룹 모드에서만 붙인다. 전체 기록에는 그룹이 여럿이라
             어느 그룹의 순위로 보내야 할지 정할 수 없다.
             빈 상태·실패에도 남긴다 — 기록이 0건이어도 순위는 있다.
        -->
        <GroupHonorCourtEntry
            v-if="!isAllMode && !loading"
            class="trial-records__honor"
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

.trial-records__scope {
    margin-bottom: var(--tt-space-3);
}

.trial-records__summary {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-2);
    padding: 0 4px var(--tt-space-3);
}
.trial-records__counts {
    display: flex;
    align-items: baseline;
    gap: 7px;
    min-width: 0;
}
.trial-records__total {
    flex: none;
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}
/* 읽히되 세지 않는다. 숫자보다 한 단계 작고 흐리다 */
.trial-records__sort {
    flex: none;
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-hint);
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
