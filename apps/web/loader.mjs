import { fileURLToPath, pathToFileURL } from 'node:url';
import { resolve as resolvePath, basename } from 'node:path';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const srcPath = resolvePath(__dirname, 'src');

export async function resolve(specifier, context, nextResolve) {
    if (specifier.startsWith('@/')) {
        const modulePath = resolvePath(srcPath, specifier.slice(2));
        // Only append .js when the specifier has no file extension at all,
        // otherwise pass paths like '@/components/Foo.vue' through unchanged.
        const hasExtension = basename(modulePath).includes('.');
        const resolvedPath = hasExtension ? modulePath : modulePath + '.js';
        const moduleURL = pathToFileURL(resolvedPath).href;
        return nextResolve(moduleURL, context);
    }
    return nextResolve(specifier, context);
}
