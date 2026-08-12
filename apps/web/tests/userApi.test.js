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

test('업로드는 FormData 로 보내고 Content-Type 을 직접 지정하지 않는다', async () => {
    /* 손으로 지정하면 boundary 가 빠져 서버가 파싱하지 못한다 */
    stub.reset();

    await uploadMyProfileImage(new Blob(['x'], { type: 'image/jpeg' }));

    const [url, body, config] = stub.calls[0].args;
    assert.equal(stub.calls[0].method, 'post');
    assert.equal(url, '/users/me/profile-image');
    assert.ok(body instanceof FormData);
    assert.equal(body.get('file') !== null, true);
    assert.equal(config, undefined);
});

test('삭제는 같은 경로로 DELETE 를 보낸다', async () => {
    stub.reset();

    await deleteMyProfileImage();

    assert.equal(stub.calls[0].method, 'delete');
    assert.equal(stub.calls[0].args[0], '/users/me/profile-image');
});
