export const calls = [];

const getResponses = [];
const postResponses = [];

export function reset() {
    calls.length = 0;
    getResponses.length = 0;
    postResponses.length = 0;
}

export function setGetResponses(responses) {
    getResponses.push(...responses);
}

export function setPostResponses(responses) {
    postResponses.push(...responses);
}

function nextResponse(responses) {
    const response = responses.shift();
    return response instanceof Error ? Promise.reject(response) : Promise.resolve(response);
}

const http = {
    get: (...args) => {
        calls.push({ method: 'get', args });
        return nextResponse(getResponses);
    },
    post: (...args) => {
        calls.push({ method: 'post', args });
        return nextResponse(postResponses);
    },
};

export default http;
