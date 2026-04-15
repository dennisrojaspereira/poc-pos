import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { BASE_URL, signedHeaders, uuidLike } from './lib/signature.js';

export const options = {
    scenarios: {
        authorize_idempotent: {
            executor: 'per-vu-iterations',
            vus: 5,
            iterations: 5,
            exec: 'authorizeIdempotentScenario',
        },
        full_transaction_flow: {
            executor: 'per-vu-iterations',
            vus: 5,
            iterations: 5,
            exec: 'fullFlowScenario',
            startTime: '2s',
        },
    },
    thresholds: {
        checks: ['rate>0.99'],
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1500'],
    },
};

export function authorizeIdempotentScenario() {
    const tx = uuidLike('tx-reg');
    const terminalId = `term-${__VU}`;
    const nsu = uuidLike('nsu');
    const featureVariant = __VU % 2 === 0 ? 'treatment' : 'control';
    const body = JSON.stringify({
        transactionId: tx,
        terminalId,
        nsu,
        amount: 10.0,
    });

    group('authorize idempotent', () => {
        const first = http.post(`${BASE_URL}/authorize`, body, {
            headers: signedHeaders('POST', '/authorize', body, null, featureVariant),
        });
        const second = http.post(`${BASE_URL}/authorize`, body, {
            headers: signedHeaders('POST', '/authorize', body, null, featureVariant),
        });

        check(first, {
            'first authorize status 200': (r) => r.status === 200,
        });
        check(second, {
            'second authorize status 200': (r) => r.status === 200,
            'same transaction id on repeat': (r) => r.json('transactionId') === tx,
            'same nsu on repeat': (r) => r.json('nsu') === nsu,
        });
    });
}

export function fullFlowScenario() {
    const tx = uuidLike('tx-flow');
    const terminalId = `term-flow-${__VU}`;
    const nsu = uuidLike('nsu-flow');
    const featureVariant = __VU % 2 === 0 ? 'treatment' : 'control';
    const authorizeBody = JSON.stringify({
        transactionId: tx,
        terminalId,
        nsu,
        amount: 20.0,
    });
    const confirmBody = JSON.stringify({ transactionId: tx });
    const voidBody = JSON.stringify({ transactionId: tx });

    group('full transaction flow', () => {
        const authorize = http.post(`${BASE_URL}/authorize`, authorizeBody, {
            headers: signedHeaders('POST', '/authorize', authorizeBody, null, featureVariant),
        });
        const confirm = http.post(`${BASE_URL}/confirm`, confirmBody, {
            headers: signedHeaders('POST', '/confirm', confirmBody, null, featureVariant),
        });
        const voidResponse = http.post(`${BASE_URL}/void`, voidBody, {
            headers: signedHeaders('POST', '/void', voidBody, null, featureVariant),
        });

        check(authorize, {
            'authorize 200': (r) => r.status === 200,
        });
        check(confirm, {
            'confirm 204': (r) => r.status === 204,
        });
        check(voidResponse, {
            'void 204': (r) => r.status === 204,
        });
    });

    sleep(0.2);
}
