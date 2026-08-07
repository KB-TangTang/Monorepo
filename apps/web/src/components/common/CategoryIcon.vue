<!--
  용도: 카테고리 아이콘 키 하나를 받아 heroicons 아이콘을 렌더한다.
  언제 쓰는지: 카테고리와 관련된 곳(거래내역 행 아바타, 카테고리 선택 팝업 타일)에서 공통으로 쓴다.
-->
<script setup>
import {
    AcademicCapIcon,
    BanknotesIcon,
    BuildingLibraryIcon,
    CakeIcon,
    DevicePhoneMobileIcon,
    EllipsisHorizontalCircleIcon,
    FilmIcon,
    GiftIcon,
    HeartIcon,
    HomeIcon,
    PaperAirplaneIcon,
    ReceiptRefundIcon,
    ShieldCheckIcon,
    ShoppingBagIcon,
    ShoppingCartIcon,
    SparklesIcon,
    TruckIcon,
    WalletIcon,
} from '@heroicons/vue/24/solid';

const ICON_MAP = {
    AcademicCap: AcademicCapIcon,
    Banknotes: BanknotesIcon,
    BuildingLibrary: BuildingLibraryIcon,
    Cake: CakeIcon,
    DevicePhoneMobile: DevicePhoneMobileIcon,
    EllipsisHorizontalCircle: EllipsisHorizontalCircleIcon,
    Film: FilmIcon,
    Gift: GiftIcon,
    Heart: HeartIcon,
    Home: HomeIcon,
    PaperAirplane: PaperAirplaneIcon,
    ReceiptRefund: ReceiptRefundIcon,
    ShieldCheck: ShieldCheckIcon,
    ShoppingBag: ShoppingBagIcon,
    ShoppingCart: ShoppingCartIcon,
    Sparkles: SparklesIcon,
    Truck: TruckIcon,
    Wallet: WalletIcon,
};

const props = defineProps({
    icon: { type: String, required: true },
});
</script>

<template>
    <component
        :is="ICON_MAP[props.icon] ?? EllipsisHorizontalCircleIcon"
        class="category-icon"
        aria-hidden="true"
    />
</template>

<style scoped>
.category-icon {
    width: 22px;
    height: 22px;
}
</style>
