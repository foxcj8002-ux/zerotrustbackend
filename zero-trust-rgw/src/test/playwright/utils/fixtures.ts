import * as fs from 'fs';
import * as path from 'path';
import * as crypto from 'crypto';
import * as jwt from 'jsonwebtoken';

const PRIVATE_KEY_PATH = path.resolve(__dirname, './casdoor-key.pem');

let privateKeyPem: string;
try {
  privateKeyPem = fs.readFileSync(PRIVATE_KEY_PATH, 'utf8');
} catch (error) {
  console.warn(`Warning: Private key not found at ${PRIVATE_KEY_PATH}. JWT generation will fail.`);
  privateKeyPem = '';
}

export interface JwtPayload {
  sub: string;        // userId
  resourceId?: string;
  exp?: number;
}

export function generateTestJwt(payload: JwtPayload): string {
  if (!privateKeyPem) {
    throw new Error(`Private key not found at ${PRIVATE_KEY_PATH}`);
  }

  const options: jwt.SignOptions = {
    algorithm: 'RS256',
    header: { alg: 'RS256', typ: 'JWT' },
  };

  if (payload.exp) {
    options.expiresIn = Math.floor((payload.exp - Date.now()) / 1000);
  } else {
    options.expiresIn = 3600; // 1 hour
  }

  const token = jwt.sign(
    {
      sub: payload.sub,
      ...(payload.resourceId && { resourceId: payload.resourceId }),
    },
    privateKeyPem,
    options
  );

  return token;
}

export function generateExpiredJwt(userId: string, resourceId?: string): string {
  const now = Math.floor(Date.now() / 1000);
  const iat = now - 7200;  // 2 hours ago
  const exp = now - 3600;   // 1 hour ago

  const token = jwt.sign(
    {
      sub: userId,
      iat: iat,
      exp: exp,
      ...(resourceId && { resourceId }),
    },
    privateKeyPem,
    {
      algorithm: 'RS256',
      header: { alg: 'RS256', typ: 'JWT' },
    }
  );

  return token;
}

export const TEST_USERS = {
  user001: {
    userId: 'user-001',
    resourceId: 'baidu-test',
    sessionId: 'session-001',
  },
  user002: {
    userId: 'user-002',
    resourceId: 'jsonplaceholder-1',
    sessionId: 'session-002',
  },
  mismatchUser: {
    userId: 'user-999',
    resourceId: 'baidu-test',
    sessionId: 'session-mismatch',
  },
};

export const TEST_RESOURCES = {
  baidu: {
    resourceId: 'baidu-test',
    name: '百度搜索',
    url: 'https://www.baidu.com',
  },
  jsonplaceholder: {
    resourceId: 'jsonplaceholder-1',
    name: 'JSONPlaceholder API',
    url: 'https://jsonplaceholder.typicode.com',
  },
  reqres: {
    resourceId: 'reqres-1',
    name: 'ReqRes API',
    url: 'https://reqres.in',
  },
  ssrf_localhost: {
    resourceId: 'ssrf-localhost',
    name: 'SSRF Test Localhost',
    url: 'http://127.0.0.1:8080',
  },
  ssrf_private_10: {
    resourceId: 'ssrf-private-10',
    name: 'SSRF Test 10.x',
    url: 'http://10.0.0.1/admin',
  },
  ssrf_private_192: {
    resourceId: 'ssrf-private-192',
    name: 'SSRF Test 192.168.x',
    url: 'http://192.168.1.100/api',
  },
  ssrf_metadata: {
    resourceId: 'ssrf-metadata',
    name: 'SSRF Test Metadata',
    url: 'http://169.254.169.254/latest/meta-data/',
  },
};
