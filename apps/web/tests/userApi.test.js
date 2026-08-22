import test from 'node:test';
import assert from 'node:assert/strict';
import { registerHooks } from 'node:module';

const STUB_URL = new URL('./stubs/httpStub.js', import.meta.url).href;

/*
 * 훅은 등록 이후 로드되는 모듈에만 적용되므로 대상 모듈을 동적 import 로 나중에 부른다.
 * (tests/tutorialGuide.test.js 와 같은 방식)
 */
registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/http') {
            return { url: STUB_URL, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const stub = await import('./stubs/httpStub.js');
const { uploadMyProfileImage, deleteMyProfileImage } = await import('../src/api/user.js');

test('업로드는 FormData 로 보내고 인스턴스 기본 Content-Type 을 명시적으로 지운다', async () => {
    /*
     * http.js 인스턴스는 기본 헤더로 Content-Type: application/json 을 박아둔다. 아무것도
     * 넘기지 않으면 그 기본값이 살아남아 axios 가 FormData 를 JSON.stringify 로 바꿔버리고
     * 파일이 브라우저를 떠나지 못한다(실측) — 그래서 undefined 를 "명시적으로" 전달해
     * 인스턴스 기본값을 지우는지를 검증한다. config 가 undefined 인 것과는 다르다.
     */
    stub.reset();

    await uploadMyProfileImage(new Blob(['x'], { type: 'image/jpeg' }));

    const [url, body, config] = stub.calls[0].args;
    assert.equal(stub.calls[0].method, 'post');
    assert.equal(url, '/users/me/profile-image');
    assert.ok(body instanceof FormData);
    assert.equal(body.get('file') !== null, true);
    assert.ok(config && 'headers' in config, 'config.headers 가 명시적으로 전달되어야 한다');
    assert.equal(config.headers['Content-Type'], undefined);
    assert.ok(
        Object.prototype.hasOwnProperty.call(config.headers, 'Content-Type'),
        'Content-Type 키 자체가 없으면 인스턴스 기본값을 지우지 못한다',
    );
});

test('삭제는 같은 경로로 DELETE 를 보낸다', async () => {
    stub.reset();

    await deleteMyProfileImage();

    assert.equal(stub.calls[0].method, 'delete');
    assert.equal(stub.calls[0].args[0], '/users/me/profile-image');
});
