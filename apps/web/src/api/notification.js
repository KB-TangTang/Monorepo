import http from './http';

/** 최신순 목록. cursor 가 null 이면 처음부터. */
export function fetchNotifications(cursor = null, size = 20) {
    const params = { size };
    if (cursor) {
        params.cursor = cursor;
    }
    return http.get('/notifications', { params });
}

export function fetchUnreadCount() {
    return http.get('/notifications/unread-count');
}

export function markNotificationRead(id) {
    return http.post(`/notifications/${id}/read`);
}

export function markAllNotificationsRead() {
    return http.post('/notifications/read-all');
}

/** SSE 스트림 주소. EventSource 를 쓰지 않으므로 fetch 대상 URL 만 만든다. */
export const NOTIFICATION_STREAM_URL = '/api/notifications/stream';
