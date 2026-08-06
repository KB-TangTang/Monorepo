import { fileURLToPath, pathToFileURL } from 'node:url';
import { resolve as resolvePath } from 'node:path';
import { existsSync } from 'node:fs';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const srcPath = resolvePath(__dirname, 'src');

export async function resolve(specifier, context, nextResolve) {
    if (specifier.startsWith('@/')) {
        const modulePath = resolvePath(srcPath, specifier.slice(2));
        // Try with .js extension if the path doesn't have an extension
        const pathWithJs = modulePath.endsWith('.js') ? modulePath : modulePath + '.js';
        const moduleURL = pathToFileURL(pathWithJs).href;
        return nextResolve(moduleURL, context);
    }
    return nextResolve(specifier, context);
}
