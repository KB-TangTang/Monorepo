/*
 * 그룹 챌린지 대상 카테고리 (이슈 #352).
 *
 * ⚠ 여기 있는 것은 **소분류 이름**이다. 대분류를 넣으면 안 된다.
 *
 * 그룹 챌린지 소비 집계는 `tbl_transaction.category_id = tbl_challenge_group.category_id`
 * 정확 일치로 돈다. 그런데 거래에는 **소분류 id 만** 붙는다 — 대분류 id 를 저장하면 매칭되는
 * 거래가 한 건도 없어 **집계가 늘 0원**이 되고, 한도를 아무리 넘겨도 아무도 기소되지 않는다.
 * 만들기 화면이 대분류 id(1~5)를 박아 두고 있어서 실제로 그 상태였다.
 *
 * **id 를 박지 않고 이름으로 찾는다.** `tbl_category` 는 AUTO_INCREMENT 라 로컬·도커·배포에서
 * id 가 서로 다를 수 있다. id 를 박으면 어느 한 환경에서 조용히 엉뚱한 카테고리가 걸린다.
 * 이름은 `db/seed_category.sql` 이 정하고 시드가 그 이름으로 서로를 참조하므로 환경이 달라도 같다.
 *
 * 서버에 없는 이름은 조용히 건너뛴다. 시드를 안 돌린 환경에서 화면이 통째로 죽는 것보다
 * 고를 수 있는 것만 보이는 편이 낫다.
 */

/**
 * 카테고리를 고르지 않은 그룹(`categoryId` 가 NULL)의 표시 이름.
 *
 * **「기타」가 아니다.** 전체 소비를 합산한다는 뜻이라 뜻이 정반대다.
 * 만들기 화면과 목록이 서로 다른 말을 쓰지 않도록 여기 한 곳에서만 정한다.
 */
export const GROUP_CATEGORY_ALL_LABEL = '총 소비';

/** 고를 수 있는 소분류 15개. **배열 순서가 화면 순서다.** */
export const GROUP_CATEGORY_NAMES = [
    /* 식비 */
    '음식점/외식',
    '배달앱',
    '카페/간식',
    '편의점',
    /* 쇼핑 */
    '온라인쇼핑',
    '패션',
    '뷰티',
    /* 교통 */
    '택시/모빌리티',
    '주유/충전',
    '주차/통행료',
    /* 생활 */
    '장보기/마트',
    /* 문화/여가 */
    '영화·공연·전시',
    '도서',
    '취미',
    '레저',
];

/**
 * `GET /api/categories` 응답을 대분류 섹션으로 묶는다.
 *
 * 15개를 한 줄로 늘어놓으면 무엇을 고르는지 알아보기 어렵다. 대분류로 끊어 보여준다.
 * 대분류 이름도 서버 응답(`parentId` 로 이어진 행)에서 가져온다 — 여기에 또 적어 두면
 * 시드에서 대분류 이름을 바꿨을 때 화면만 옛 이름으로 남는다.
 *
 * @param {{id:number, name:string, parentId:number|null}[]} categories
 * @returns {{parentId:number, parentName:string, items:{id:number,name:string}[]}[]}
 */
export function toGroupCategorySections(categories = []) {
    const byId = new Map(categories.map((category) => [category.id, category]));
    const sections = [];
    const sectionByParentId = new Map();

    for (const name of GROUP_CATEGORY_NAMES) {
        /* parentId 가 있는 행만 본다. 같은 이름의 대분류가 있어도 소분류를 고른다. */
        const found = categories.find(
            (category) => category.name === name && category.parentId != null,
        );
        if (!found) continue;

        const parent = byId.get(found.parentId);
        if (!parent) continue;

        let section = sectionByParentId.get(parent.id);
        if (!section) {
            section = { parentId: parent.id, parentName: parent.name, items: [] };
            sectionByParentId.set(parent.id, section);
            sections.push(section);
        }
        section.items.push({ id: found.id, name: found.name });
    }

    return sections;
}
