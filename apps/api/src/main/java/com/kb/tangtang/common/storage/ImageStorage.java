package com.kb.tangtang.common.storage;

/**
 * 이미지 저장소. 구현을 갈아끼워 로컬 파일 ↔ S3 를 전환한다
 * (financial.client=mock|codef 와 같은 방식 — DECISIONS.md 2026-08-05).
 *
 * **키와 URL 을 구분한다.** DB 에 저장하는 값은 키이고, URL 은 이 인터페이스가 조립한다.
 * 그래야 저장소를 바꿔도 기존 행을 변환할 필요가 없다.
 */
public interface ImageStorage {

    /**
     * 저장한다. 같은 키가 이미 있으면 덮어쓴다.
     *
     * @return 저장된 키(넘긴 키를 그대로 돌려준다 — 호출부가 DB 에 넣을 값이다)
     */
    String store(byte[] content, String key);

    /** 지운다. **없는 키를 지워도 예외를 던지지 않는다**(멱등). key 가 null 이면 아무것도 하지 않는다. */
    void delete(String key);

    /** 키를 화면이 쓸 URL 로 바꾼다. key 가 null 이면 null 이다. */
    String urlOf(String key);
}
