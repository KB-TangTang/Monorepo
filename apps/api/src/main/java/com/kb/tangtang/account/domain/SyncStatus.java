package com.kb.tangtang.account.domain;

/**
 * 연결 계좌의 동기화 상태.
 * 값은 tbl_connected_account.sync_status 컬럼과 1:1 이다. (db/schema.sql 이 기준)
 */
public enum SyncStatus {

    NORMAL,
    SYNCING,
    NEED_SYNC,
    FAILED,
    NEED_RECONNECT
}
