<!--
  용도: 홈 탭 최상단의 풍경 히어로 헤더. 하늘 → 잔디 그라데이션 위에 구름·나무·수풀·꽃을
        깔고, 잔디 위를 탕이가 좌우로 걷는다. 그 위에 인사말과 알림 벨만 얹는다.
  언제 쓰는지: /home 최상단. 홈은 이 헤더 아래로 카드가 스크롤된다.
  쓰면 안 되는 경우: 재판탭 홈. 그쪽은 다크 헤더(ChallengeCourtHeader)를 쓴다.

  **이 헤더는 정보를 담지 않는다.** 숫자·상태·버튼은 전부 아래 본문 카드가 받는다.
  여기에 지표를 얹기 시작하면 스크롤 없이 보이는 첫 화면이 다시 정보판이 된다.

  좌표는 전부 디자인 원본(390x500 기준) 값 그대로다 —
  docs.local/reference/design/home/메인_홈_애니메이션_ver.html
  원본의 `9:41` 상태바 줄은 목업용 가짜 크롬이라 그리지 않는다(절대배치라 높이에 영향 없음).
  대신 설치형에서 시계 줄이 올라앉을 자리를 하늘색 스트립으로 채운다.
-->
<script setup>
import TheNotificationBell from '@/components/common/TheNotificationBell.vue';
import tangiWalking from '@/assets/images/emotions/03_gentle_smile.png';
import bench from '@/assets/images/home_scene/bench.png';
import bushLg from '@/assets/images/home_scene/bush_lg.png';
import bushSm from '@/assets/images/home_scene/bush_sm.png';
import cloudLg from '@/assets/images/home_scene/cloud_lg.png';
import cloudMd from '@/assets/images/home_scene/cloud_md.png';
import cloudSm from '@/assets/images/home_scene/cloud_sm.png';
import flowerLg from '@/assets/images/home_scene/flower_lg.png';
import flowerSm from '@/assets/images/home_scene/flower_sm.png';
import grass from '@/assets/images/home_scene/grass.png';
import leaf1 from '@/assets/images/home_scene/leaf_1.png';
import leaf2 from '@/assets/images/home_scene/leaf_2.png';
import leaf3 from '@/assets/images/home_scene/leaf_3.png';
import leaf4 from '@/assets/images/home_scene/leaf_4.png';
import leaf5 from '@/assets/images/home_scene/leaf_5.png';
import leaf6 from '@/assets/images/home_scene/leaf_6.png';
import leaf7 from '@/assets/images/home_scene/leaf_7.png';
import leaf8 from '@/assets/images/home_scene/leaf_8.png';
import leaf9 from '@/assets/images/home_scene/leaf_9.png';
import treeLg from '@/assets/images/home_scene/tree_lg.png';
import treeMd from '@/assets/images/home_scene/tree_md.png';
import treeSm from '@/assets/images/home_scene/tree_sm.png';

defineProps({
    /** 인사말에 넣을 이름. 빈 값이면 이름 없이 인사만 한다 (신규 가입 직후 등). */
    userName: { type: String, default: '' },
});
</script>

<template>
    <header class="home-hero">
        <div class="home-hero__statusbar"></div>

        <div class="home-hero__scene">
            <!--
              원본 500px 씬. 맨 위 40px 은 목업에서 가짜 `9:41` 줄이 덮고 있던 띠라
              그대로 두면 브라우저에서 빈 하늘로 남는다. __scene 이 그만큼 잘라낸다.
            -->
            <div class="home-hero__stage">
                <!-- 하늘 -->
                <div class="home-hero__sun"></div>

                <!-- 잔디. 아래로 갈수록 진해지다 본문 배경으로 빠진다 -->
                <div class="home-hero__field"></div>
                <div class="home-hero__path"></div>

                <!--
                  구름. 한 장에 트랙 하나씩이다. 트랙은 씬과 폭이 같아서
                  translateX(-100%→100%) 가 「화면 왼쪽 밖 → 오른쪽 밖」이 된다.
                  속도를 넷 다 다르게 줘서 앞뒤 거리감이 생긴다.
                -->
                <div class="home-hero__cloud-track home-hero__cloud-track--1">
                    <img :src="cloudLg" alt="" class="home-hero__cloud home-hero__cloud--1" />
                </div>
                <div class="home-hero__cloud-track home-hero__cloud-track--2">
                    <img :src="cloudMd" alt="" class="home-hero__cloud home-hero__cloud--2" />
                </div>
                <div class="home-hero__cloud-track home-hero__cloud-track--3">
                    <img :src="cloudSm" alt="" class="home-hero__cloud home-hero__cloud--3" />
                </div>
                <div class="home-hero__cloud-track home-hero__cloud-track--4">
                    <img :src="cloudSm" alt="" class="home-hero__cloud home-hero__cloud--4" />
                </div>

                <!-- 배경 나무 · 수풀 -->
                <img :src="treeSm" alt="" class="home-hero__prop home-hero__tree-sm" />
                <img :src="treeMd" alt="" class="home-hero__prop home-hero__tree-md" />
                <img :src="treeLg" alt="" class="home-hero__prop home-hero__tree-lg" />
                <img :src="bushLg" alt="" class="home-hero__prop home-hero__bush-lg" />
                <img :src="bushSm" alt="" class="home-hero__prop home-hero__bush-sm" />
                <img :src="bench" alt="" class="home-hero__prop home-hero__bench" />
                <img :src="grass" alt="" class="home-hero__prop home-hero__grass" />
                <img :src="flowerLg" alt="" class="home-hero__prop home-hero__flower-lg" />
                <img :src="flowerSm" alt="" class="home-hero__prop home-hero__flower-sm" />

                <!-- 떨어지는 잎. 시작 시각을 어긋나게 줘서 다 같이 떨어지지 않게 한다 -->
                <img
                    :src="leaf1"
                    alt=""
                    class="home-hero__prop home-hero__fall home-hero__fall--1"
                />
                <img
                    :src="leaf2"
                    alt=""
                    class="home-hero__prop home-hero__fall home-hero__fall--2"
                />
                <img
                    :src="leaf3"
                    alt=""
                    class="home-hero__prop home-hero__fall home-hero__fall--3"
                />
                <img
                    :src="leaf4"
                    alt=""
                    class="home-hero__prop home-hero__fall home-hero__fall--4"
                />
                <img
                    :src="leaf5"
                    alt=""
                    class="home-hero__prop home-hero__fall home-hero__fall--5"
                />

                <!-- 잔디에 떨어져 있는 잎 -->
                <img
                    :src="leaf6"
                    alt=""
                    class="home-hero__prop home-hero__leaf home-hero__leaf--1"
                />
                <img
                    :src="leaf7"
                    alt=""
                    class="home-hero__prop home-hero__leaf home-hero__leaf--2"
                />
                <img
                    :src="leaf2"
                    alt=""
                    class="home-hero__prop home-hero__leaf home-hero__leaf--3"
                />
                <img
                    :src="leaf8"
                    alt=""
                    class="home-hero__prop home-hero__leaf home-hero__leaf--4"
                />
                <img
                    :src="leaf9"
                    alt=""
                    class="home-hero__prop home-hero__leaf home-hero__leaf--5"
                />

                <!-- 잔디 끝을 본문 배경으로 녹이는 페이드. 탕이보다 아래에 깔린다 -->
                <div class="home-hero__fade"></div>

                <!--
                  걷는 탕이. 바깥 상자가 좌우 왕복(끝에서 좌우 반전), 안쪽이 통통 튀는 걸음.
                  두 겹으로 나눠야 반전과 걸음이 서로 transform 을 덮어쓰지 않는다.
                -->
                <div class="home-hero__walk">
                    <div class="home-hero__walk-pace">
                        <span class="home-hero__walk-shadow"></span>
                        <img :src="tangiWalking" alt="탕이" class="home-hero__tangi" />
                    </div>
                </div>

                <div class="home-hero__greeting">
                    <div>
                        <p class="home-hero__hello">
                            {{ userName ? `${userName}님, 안녕하세요 👋` : '안녕하세요 👋' }}
                        </p>
                        <p class="home-hero__slogan">오늘도 현명한 한 걸음!</p>
                    </div>

                    <div class="home-hero__bell">
                        <TheNotificationBell />
                    </div>
                </div>
            </div>
        </div>
    </header>
</template>

<style scoped src="./HomeHeroHeader.css"></style>
