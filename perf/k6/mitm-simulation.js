import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, sign, uuidLike } from './lib/signature.js';

export const options = {
    scenarios: {
        tampered_body_attack: {
            executor: 'per-vu-iterations',
            vus: 3,
            iterations: 5,
            exec: 'tamperedBodyAttack',
        },
        stripped_signature_attack: {
            executor: 'per-vu-iterations',
            vus: 2,
            iterations: 5,
            startTime: '1s',
            exec: 'strippedSignatureAttack',
        },
        replay_attack_blocked: {
            executor: 'per-vu-iterations',
            vus: 2,
            iterations: 5,
            startTime: '2s',
            exec: 'replayAttackBlocked',
        },
    },
    thresholds: {
        checks: ['rate>0.98'],
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<2000'],
    },
};

export function tamperedBodyAttack() {
    const originalBody = JSON.stringify({
        transactionId: uuidLike('tx-mitm'),
        terminalId: 'term-mitm',
        nsu: uuidLike('nsu-mitm'),
        amount: 10.0,
    });
    const tamperedBody = JSON.stringify({
        ...JSON.parse(originalBody),
        amount: 999.99,
    });
    const timestamp = new Date().toISOString();
    const correlationId = uuidLike('corr-mitm');
    const signature = sign('POST', '/authorize', timestamp, correlationId, originalBody);

    const response = http.post(`${BASE_URL}/authorize`, tamperedBody, {
        responseCallback: http.expectedStatuses(401),
        headers: {
            'Content-Type': 'application/json',
            'X-Timestamp': timestamp,
            'X-Correlation-Id': correlationId,
            'X-Feature-Variant': 'control',
            'X-Signature': signature,
        },
    });

    check(response, {
        'tampered body rejected with 401': (r) => r.status === 401,
    });
}

export function strippedSignatureAttack() {
    const body = JSON.stringify({
        transactionId: uuidLike('tx-strip'),
        terminalId: 'term-strip',
        nsu: uuidLike('nsu-strip'),
        amount: 10.0,
    });

    const response = http.post(`${BASE_URL}/authorize`, body, {
        responseCallback: http.expectedStatuses(401),
        headers: {
            'Content-Type': 'application/json',
            'X-Timestamp': new Date().toISOString(),
            'X-Correlation-Id': uuidLike('corr-strip'),
            'X-Feature-Variant': 'control',
        },
    });

    check(response, {
        'missing signature rejected with 401': (r) => r.status === 401,
    });
}

export function replayAttackBlocked() {
    const body = JSON.stringify({
        transactionId: uuidLike('tx-replay'),
        terminalId: 'term-replay',
        nsu: uuidLike('nsu-replay'),
        amount: 10.0,
    });
    const timestamp = new Date().toISOString();
    const correlationId = uuidLike('corr-replay');
    const signature = sign('POST', '/authorize', timestamp, correlationId, body);
    const headers = {
        'Content-Type': 'application/json',
        'X-Timestamp': timestamp,
        'X-Correlation-Id': correlationId,
        'X-Feature-Variant': 'control',
        'X-Signature': signature,
    };

    group('replay attack blocked', () => {
        const firstResponse = http.post(`${BASE_URL}/authorize`, body, {
            responseCallback: http.expectedStatuses(200),
            headers,
        });
        const replayResponse = http.post(`${BASE_URL}/authorize`, body, {
            responseCallback: http.expectedStatuses(401),
            headers,
        });

        check(firstResponse, {
            'first signed request accepted': (r) => r.status === 200,
        });
        check(replayResponse, {
            'replay rejected with 401': (r) => r.status === 401,
        });
    });
}
