package com.kb.tangtang.account.client.stock;

import java.util.List;

/**
 * 토스 API 응답(Map&lt;String,Object&gt;)에서 값을 꺼낼 때 쓰는 작은 공용 헬퍼(QA 지적사항으로 추출).
 *
 * ⚠ {@code CodefFinancialDataClient}·{@code MockFinancialSyncClient} 에도 이름이 비슷한 헬퍼가
 *   있지만 일부러 공유하지 않는다 — 이 커밋 밖의, 이미 팀원이 쓰고 있는 안정된 파일을 건드리는 건
 *   별개의 위험이라 이번 범위(토스 연동)에서 새로 만든 두 클래스(TossAuthClient·TossPriceClient)
 *   끼리만 우선 공유한다. 숫자 파싱({@code decimal}/{@code number})은 클래스마다 반환 타입과
 *   "파싱 실패 시 무엇을 돌려줄지"가 달라(TossPriceClient 참고) 여기 넣지 않는다.
 */
final class TossJsonSupport {

    private TossJsonSupport() {
    }

    static List<?> asList(Object value) {
        return value instanceof List ? (List<?>) value : List.of();
    }

    static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
