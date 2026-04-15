import crypto from 'k6/crypto';

export const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
export const HMAC_SECRET = __ENV.HMAC_SECRET || 'change-me';

export function uuidLike(prefix) {
    return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000000)}`;
}

export function sign(method, path, timestamp, correlationId, body) {
    const canonical = `${method}\n${path}\n${timestamp}\n${correlationId}\n${body}`;
    return crypto.hmac('sha256', HMAC_SECRET, canonical, 'hex');
}

export function signedHeaders(method, path, body, timestamp, featureVariant) {
    const finalTimestamp = timestamp || new Date().toISOString();
    const correlationId = uuidLike('corr');
    const headers = {
        'Content-Type': 'application/json',
        'X-Timestamp': finalTimestamp,
        'X-Correlation-Id': correlationId,
        'X-Signature': sign(method, path, finalTimestamp, correlationId, body),
    };
    if (featureVariant) {
        headers['X-Feature-Variant'] = featureVariant;
    }
    return headers;
}
