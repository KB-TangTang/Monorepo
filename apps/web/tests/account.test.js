import test from 'node:test';
import assert from 'node:assert/strict';
import {
    LINK_EXIT_ROUTE,
    LINK_STEPS,
    calcLinkProgress,
    canEnterLinkStep,
    consentExpiryLabel,
    formatAmount,
    formatBirthDate,
    formatCooldown,
    formatPhoneNumber,
    formatSyncTime,
    isPollExpired,
    linkStepPosition,
    needsReconnect,
    nextLinkStep,
    prevLinkDestination,
    prevLinkStep,
    resolveAuthView,
    resolveInstitutionTone,
    resolveProgressRow,
    resolveSyncBadge,
    validateSimpleAuthForm,
} from '../src/utils/account.js';

test('연결 플로우 단계를 앞뒤로 이동한다', () => {
    assert.deepEqual(LINK_STEPS, ['institutions', 'auth', 'progress', 'select', 'done']);
    assert.equal(nextLinkStep('institutions'), 'auth');
    assert.equal(nextLinkStep('auth'), 'progress');
    assert.equal(nextLinkStep('select'), 'done');
    assert.equal(prevLinkStep('select'), 'progress');
});

test('첫 단계의 이전과 마지막 단계의 다음은 없다', () => {
    assert.equal(prevLinkStep('institutions'), null);
    assert.equal(nextLinkStep('done'), null);
});

test('모르는 단계는 이동 대상이 아니다', () => {
    assert.equal(nextLinkStep('없는단계'), null);
    assert.equal(prevLinkStep('없는단계'), null);
});

test('진행 표시는 1부터 센다', () => {
    assert.deepEqual(linkStepPosition('institutions'), { current: 1, total: 5 });
    assert.deepEqual(linkStepPosition('done'), { current: 5, total: 5 });
});

test('중간 단계는 이전 단계 결과가 있어야 들어갈 수 있다', () => {
    const empty = { selectedCount: 0, hasConnection: false, linkedCount: 0, progressDone: false };
    assert.equal(canEnterLinkStep('auth', empty), false);
    assert.equal(canEnterLinkStep('select', empty), false);
    assert.equal(canEnterLinkStep('done', empty), false);

    const selected = {
        selectedCount: 2,
        hasConnection: false,
        linkedCount: 0,
        progressDone: false,
    };
    assert.equal(canEnterLinkStep('auth', selected), true);
    assert.equal(canEnterLinkStep('progress', selected), false);

    const connected = {
        selectedCount: 2,
        hasConnection: true,
        linkedCount: 0,
        progressDone: false,
    };
    assert.equal(canEnterLinkStep('progress', connected), true);
    assert.equal(canEnterLinkStep('done', connected), false);

    const linked = { selectedCount: 2, hasConnection: true, linkedCount: 3, progressDone: true };
    assert.equal(canEnterLinkStep('done', linked), true);
});

test('조회가 끝나야 계좌 선택 단계에 들어갈 수 있다', () => {
    const base = { selectedCount: 1, hasConnection: true, linkedCount: 0, progressDone: false };
    assert.equal(canEnterLinkStep('progress', base), true);
    assert.equal(canEnterLinkStep('select', base), false);
    assert.equal(canEnterLinkStep('select', { ...base, progressDone: true }), true);
});

test('첫 단계는 언제나 들어갈 수 있다', () => {
    const empty = { selectedCount: 0, hasConnection: false, linkedCount: 0, progressDone: false };
    assert.equal(canEnterLinkStep('institutions', empty), true);
});

test('인증 수단이 하나면 선택 화면을 건너뛴다', () => {
    assert.deepEqual(resolveAuthView([{ type: 'SIMPLE_AUTH', providers: [] }]), {
        needsPicker: false,
        method: 'SIMPLE_AUTH',
    });
    assert.deepEqual(resolveAuthView([{ type: 'SIMPLE_AUTH' }, { type: 'INSTITUTION_LOGIN' }]), {
        needsPicker: true,
        method: null,
    });
    assert.deepEqual(resolveAuthView([]), { needsPicker: false, method: null });
    assert.deepEqual(resolveAuthView(undefined), { needsPicker: false, method: null });
});

test('기관별 상태로 진행률을 계산한다', () => {
    const rows = [{ status: 'DONE' }, { status: 'FETCHING' }, { status: 'WAITING' }];
    assert.equal(calcLinkProgress(rows), 33);
    assert.equal(calcLinkProgress([{ status: 'DONE' }, { status: 'DONE' }]), 100);
    assert.equal(calcLinkProgress([]), 0);
    /* 실패도 끝난 것으로 센다 — 진행률이 영원히 멈춰 있으면 화면이 죽은 것처럼 보인다. */
    assert.equal(calcLinkProgress([{ status: 'FAILED' }, { status: 'WAITING' }]), 50);
});

test('진행 상태를 아이콘·라벨로 바꾼다', () => {
    assert.equal(resolveProgressRow('DONE').label, '완료');
    assert.equal(resolveProgressRow('FETCHING').label, '조회중');
    assert.equal(resolveProgressRow('WAITING').label, '대기');
    assert.equal(resolveProgressRow('FAILED').variant, 'guilty');
    assert.equal(resolveProgressRow('없는값').label, '대기');
});

test('폴링 제한 시간을 넘겼는지 판정한다', () => {
    const start = new Date('2026-08-05T10:00:00');
    /* 기본값은 서버가 주는 인증 유효시간(5분)과 같다. 화면 카운트다운과 어긋나면 안 된다. */
    assert.equal(isPollExpired(start, new Date('2026-08-05T10:04:59')), false);
    assert.equal(isPollExpired(start, new Date('2026-08-05T10:05:00')), true);
    /* 서버가 다른 값을 주면 그걸 따른다. */
    assert.equal(isPollExpired(start, new Date('2026-08-05T10:00:30'), 20), true);
    assert.equal(isPollExpired(start, new Date('2026-08-05T10:00:30'), 60), false);
    assert.equal(isPollExpired('말이안되는값', new Date()), true);
});

test('간편인증 입력 형식을 검증한다', () => {
    const ok = { name: '홍길동', birthDate: '990101', carrier: 'SKT', phone: '01012345678' };
    assert.equal(validateSimpleAuthForm(ok).valid, true);
    assert.equal(validateSimpleAuthForm({ ...ok, birthDate: '9901' }).valid, false);
    assert.equal(validateSimpleAuthForm({ ...ok, phone: '02012345678' }).valid, false);
    assert.equal(validateSimpleAuthForm({ ...ok, carrier: '' }).valid, false);
    /* 하이픈은 허용한다 — 사용자가 넣는 걸 막을 이유가 없다. */
    assert.equal(validateSimpleAuthForm({ ...ok, phone: '010-1234-5678' }).valid, true);
    assert.equal(
        validateSimpleAuthForm({ ...ok, birthDate: '9901a1' }).errors.birthDate,
        '생년월일 6자리를 입력해주세요',
    );
    assert.equal(validateSimpleAuthForm().valid, false);
});

/*
 * 이름은 나머지 세 값과 달리 tbl_user.name 에 저장된다 — 틀린 값이 그대로 남으면
 * 다음 인증에서도 같은 이유로 실패한다. (DECISIONS.md 2026-08-11)
 */
test('간편인증 이름을 검증한다', () => {
    const ok = { name: '홍길동', birthDate: '990101', carrier: 'SKT', phone: '01012345678' };
    assert.equal(validateSimpleAuthForm({ ...ok, name: '  홍길동  ' }).valid, true);
    assert.equal(validateSimpleAuthForm({ ...ok, name: 'John Doe' }).valid, true);

    assert.equal(validateSimpleAuthForm({ ...ok, name: '' }).errors.name, '이름을 입력해주세요');
    /* 공백만 친 것도 빈 값이다. */
    assert.equal(validateSimpleAuthForm({ ...ok, name: '   ' }).errors.name, '이름을 입력해주세요');
    assert.equal(
        validateSimpleAuthForm({ ...ok, name: undefined }).errors.name,
        '이름을 입력해주세요',
    );

    assert.equal(
        validateSimpleAuthForm({ ...ok, name: '홍' }).errors.name,
        '이름을 2~50자로 입력해주세요',
    );
    /* 상한 50 은 tbl_user.name VARCHAR(50) 때문이다. */
    assert.equal(validateSimpleAuthForm({ ...ok, name: '가'.repeat(50) }).valid, true);
    assert.equal(
        validateSimpleAuthForm({ ...ok, name: '가'.repeat(51) }).errors.name,
        '이름을 2~50자로 입력해주세요',
    );

    assert.equal(
        validateSimpleAuthForm({ ...ok, name: '홍길동2' }).errors.name,
        '한글 또는 영문으로 입력해주세요',
    );
    assert.equal(
        validateSimpleAuthForm({ ...ok, name: '홍길동!' }).errors.name,
        '한글 또는 영문으로 입력해주세요',
    );
    /* 자모만 친 입력은 `가-힣` 범위 밖이라 걸러진다. */
    assert.equal(
        validateSimpleAuthForm({ ...ok, name: 'ㅎㄱㄷ' }).errors.name,
        '한글 또는 영문으로 입력해주세요',
    );
});

test('동기화 상태 5종을 배지로 옮긴다', () => {
    assert.deepEqual(resolveSyncBadge('NORMAL'), { label: '정상', variant: 'innocent' });
    assert.deepEqual(resolveSyncBadge('SYNCING'), { label: '갱신 중', variant: 'progress' });
    assert.deepEqual(resolveSyncBadge('NEED_SYNC'), { label: '갱신 필요', variant: 'default' });
    assert.deepEqual(resolveSyncBadge('FAILED'), { label: '실패', variant: 'guilty' });
    assert.deepEqual(resolveSyncBadge('NEED_RECONNECT'), {
        label: '재연동 필요',
        variant: 'guilty',
    });
});

test('모르는 동기화 상태도 배지를 만든다', () => {
    assert.deepEqual(resolveSyncBadge('WHATEVER'), { label: '알 수 없음', variant: 'default' });
});

test('실패·재연동 상태에서만 재연동 버튼을 띄운다', () => {
    assert.equal(needsReconnect('NEED_RECONNECT'), true);
    assert.equal(needsReconnect('FAILED'), true);
    assert.equal(needsReconnect('NORMAL'), false);
    assert.equal(needsReconnect('NEED_SYNC'), false);
});

test('쿨다운 남은 시간을 MM:SS 로 만든다', () => {
    assert.equal(formatCooldown(150), '02:30');
    assert.equal(formatCooldown(59), '00:59');
    assert.equal(formatCooldown(3600), '60:00');
});

test('쿨다운이 끝났거나 값이 이상하면 00:00 이다', () => {
    assert.equal(formatCooldown(0), '00:00');
    assert.equal(formatCooldown(-10), '00:00');
    assert.equal(formatCooldown(null), '00:00');
    assert.equal(formatCooldown(undefined), '00:00');
});

test('오늘 동기화한 시각은 "오늘 HH:MM" 으로 보여준다', () => {
    const now = new Date(2026, 7, 5, 14, 0);
    assert.equal(formatSyncTime('2026-08-05T09:32:00', now), '오늘 09:32');
});

test('다른 날 동기화한 시각은 날짜까지 보여준다', () => {
    const now = new Date(2026, 7, 5, 14, 0);
    assert.equal(formatSyncTime('2026-08-03T18:00:00', now), '8월 3일 18:00');
});

test('동기화 이력이 없으면(최초 연결 직후) 그렇게 알린다', () => {
    // tbl_connected_account.last_sync_at 은 최초 연결 직후 NULL 이다.
    assert.equal(formatSyncTime(null), '동기화 이력 없음');
    assert.equal(formatSyncTime('말이 안 되는 값'), '동기화 이력 없음');
});

test('동의 만료 30일 전부터만 안내한다', () => {
    const now = new Date(2026, 7, 5);
    assert.equal(consentExpiryLabel('2026-08-17T00:00:00', now), '동의 D-12');
    assert.equal(consentExpiryLabel('2027-02-01T00:00:00', now), null);
});

test('이미 만료된 동의는 남은 일수를 세지 않는다', () => {
    const now = new Date(2026, 7, 5);
    assert.equal(consentExpiryLabel('2026-08-01T00:00:00', now), '동의 만료');
});

test('만료일이 없으면 안내하지 않는다', () => {
    assert.equal(consentExpiryLabel(null), null);
    assert.equal(consentExpiryLabel('말이 안 되는 값'), null);
});

test('금액에 천 단위 구분을 넣는다', () => {
    /* 참고화면은 단위 '원' 없이 모노 숫자만 쓴다 (0-5 연결계좌선택). */
    assert.equal(formatAmount(8340000), '8,340,000');
    assert.equal(formatAmount(0), '0');
    assert.equal(formatAmount(null), '0');
});

test('기관마다 로고 색조가 다르다 (Figma 확정본)', () => {
    assert.equal(resolveInstitutionTone('0004'), 'gold'); // KB국민
    assert.equal(resolveInstitutionTone('0090'), 'gold'); // 카카오뱅크
    assert.equal(resolveInstitutionTone('0088'), 'blue'); // 신한
    assert.equal(resolveInstitutionTone('0092'), 'blue'); // 토스뱅크
    assert.equal(resolveInstitutionTone('0020'), 'green'); // 우리
    assert.equal(resolveInstitutionTone('0081'), 'rose'); // 하나
});

test('매핑에 없는 기관은 업권으로 색조를 정한다', () => {
    assert.equal(resolveInstitutionTone('0381'), 'blue'); // 카드
    assert.equal(resolveInstitutionTone('0240'), 'green'); // 증권
    assert.equal(resolveInstitutionTone(null), 'gold');
    // 보험(05)은 연동 범위에서 빠졌다. 남은 업권이 아니므로 기본 색조로 떨어진다
    assert.equal(resolveInstitutionTone('0501'), 'gold');
});

test('휴대폰번호는 입력하는 대로 하이픈이 붙는다', () => {
    assert.equal(formatPhoneNumber('01012345678'), '010-1234-5678');
    // 사용자가 하이픈을 직접 쳐도 결과가 같다
    assert.equal(formatPhoneNumber('010-1234-5678'), '010-1234-5678');
    assert.equal(formatPhoneNumber('010'), '010');
    assert.equal(formatPhoneNumber('0101'), '010-1');
    assert.equal(formatPhoneNumber('0101234'), '010-1234');
    // 010 은 타이핑 도중에도 3-4-4 를 유지한다 (하이픈이 튀지 않게)
    assert.equal(formatPhoneNumber('0101234567'), '010-1234-567');
    // 010 이 아닌 구 번호는 10자리 3-3-4
    assert.equal(formatPhoneNumber('0111234567'), '011-123-4567');
    // 숫자가 아닌 문자와 11자리 초과분은 버린다
    assert.equal(formatPhoneNumber('010abc12345678'), '010-1234-5678');
    assert.equal(formatPhoneNumber(''), '');
    assert.equal(formatPhoneNumber(null), '');
});

test('생년월일은 숫자 6자리로만 받는다', () => {
    assert.equal(formatBirthDate('990101'), '990101');
    assert.equal(formatBirthDate('99-01-01'), '990101');
    assert.equal(formatBirthDate('9901019999'), '990101');
    assert.equal(formatBirthDate('99a1b1'), '9911');
    assert.equal(formatBirthDate(null), '');
});

test('첫 단계의 뒤로가기는 히스토리 대신 연결 계좌 관리로 나간다', () => {
    // router.back() 을 쓰면 restartFlow 로 되돌아온 경우 히스토리 뒤의 auth/progress/select 로 가는데,
    // 그 화면들은 가드가 institutions 로 되돌려보내 화면이 그대로인 것처럼 보인다.
    assert.deepEqual(prevLinkDestination('institutions'), {
        type: 'route',
        name: LINK_EXIT_ROUTE,
    });
    assert.equal(LINK_EXIT_ROUTE, 'connectedAccounts');
});

test('두 번째 단계부터는 이전 단계로 간다', () => {
    assert.deepEqual(prevLinkDestination('auth'), { type: 'step', step: 'institutions' });
    assert.deepEqual(prevLinkDestination('progress'), { type: 'step', step: 'auth' });
    assert.deepEqual(prevLinkDestination('select'), { type: 'step', step: 'progress' });
    assert.deepEqual(prevLinkDestination('done'), { type: 'step', step: 'select' });
});

test('모르는 단계면 플로우 밖으로 내보낸다', () => {
    assert.deepEqual(prevLinkDestination('알수없음'), { type: 'route', name: LINK_EXIT_ROUTE });
});
