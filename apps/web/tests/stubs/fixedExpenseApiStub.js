export const calls = [];

const getResponses = [];
const patchResponses = [];
const postResponses = [];
const deleteResponses = [];

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
    patchResponses.length = 0;
    postResponses.length = 0;
    deleteResponses.length = 0;
}

export function setGetResponses(responses) {
    getResponses.push(...responses);
}

export function setPatchResponses(responses) {
    patchResponses.push(...responses);
}

export function setPostResponses(responses) {
    postResponses.push(...responses);
}

export function setDeleteResponses(responses) {
    deleteResponses.push(...responses);
}

function nextResponse(responses) {
    const response = responses.shift();
    return response instanceof Error ? Promise.reject(response) : Promise.resolve(response);
}

export default {
    get: (...args) => {
        calls.push({ method: 'get', args });
        return nextResponse(getResponses);
    },
    patch: (...args) => {
        calls.push({ method: 'patch', args });
        return nextResponse(patchResponses);
    },
    post: (...args) => {
        calls.push({ method: 'post', args });
        return nextResponse(postResponses);
    },
    delete: (...args) => {
        calls.push({ method: 'delete', args });
        return nextResponse(deleteResponses);
    },
};
