import { ASSET_SUMMARY } from '@/fixtures/asset';

function clone(value) {
    return JSON.parse(JSON.stringify(value));
}

export async function fetchAssetSummary() {
    return clone(ASSET_SUMMARY);
}
