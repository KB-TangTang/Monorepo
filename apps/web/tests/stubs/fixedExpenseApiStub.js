export const calls = [];

const getResponses = [];

export class ApiError extends Error {
    constructor(code, message, status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}

export function reset() {
    calls.length = 0;
    getResponses.length = 0;
}

export function setGetResponses(responses) {
    getResponses.push(...responses);
}

function nextResponse() {
    const response = getResponses.shift();
    return response instanceof Error ? Promise.reject(response) : Promise.resolve(response);
}

export default {
    get: (...args) => {
        calls.push({ method: 'get', args });
        return nextResponse();
    },
};
