import { test, expect } from '@playwright/test';
import { generateTestJwt, generateExpiredJwt, TEST_RESOURCES } from '../utils/fixtures';

/**
 * Test Suite: 01-authentication.spec.ts
 * 测试目标: 验证 JWT Token 认证机制
 * - TC01: 缺少 Token
 * - TC02: 无效 Token
 * - TC03: 过期 Token
 */

test.describe('认证测试套件 - Authentication Tests', () => {

  /**
   * TC01: 缺少 Token - 返回 401
   */
  test('TC01: 缺少 Token - 应该返回 401 Unauthorized', async ({ request }) => {
    const response = await request.get(`/res/${TEST_RESOURCES.baidu.resourceId}/`);
    expect(response.status()).toBe(401);
    expect(response.headers()['x-error-message']).toBe('Missing Access Token');
  });

  /**
   * TC02: 无效 Token - 返回 401
   */
  test('TC02: 无效 Token - 应该返回 401 Unauthorized', async ({ request }) => {
    const response = await request.get(`/res/${TEST_RESOURCES.baidu.resourceId}/`, {
      headers: {
        Cookie: 'access_token=invalid.jwt.token',
      },
    });
    expect(response.status()).toBe(401);
    expect(response.headers()['x-error-message']).toBe('Invalid Access Token');
  });

  /**
   * TC03: 过期 Token - 返回 401
   */
  test('TC03: 过期 Token - 应该返回 401 Unauthorized', async ({ request }) => {
    const expiredToken = generateExpiredJwt('user-001');

    const response = await request.get(`/res/${TEST_RESOURCES.baidu.resourceId}/`, {
      headers: {
        Cookie: `access_token=${expiredToken}`,
      },
    });

    expect(response.status()).toBe(401);
    expect(response.headers()['x-error-message']).toBe('Invalid Access Token');
  });

  /**
   * TC01-B: 缺少 Token 访问 JSONPlaceholder
   */
  test('TC01-B: 缺少 Token 访问 API - 应该返回 401', async ({ request }) => {
    const response = await request.get(`/res/${TEST_RESOURCES.jsonplaceholder.resourceId}/posts`);
    expect(response.status()).toBe(401);
    expect(response.headers()['x-error-message']).toBe('Missing Access Token');
  });

  /**
   * TC02-B: 空字符串 Token
   */
  test('TC02-B: 空字符串 Token - 应该返回 401', async ({ request }) => {
    const response = await request.get(`/res/${TEST_RESOURCES.baidu.resourceId}/`, {
      headers: {
        Cookie: 'access_token=',
      },
    });
    expect(response.status()).toBe(401);
    expect(response.headers()['x-error-message']).toBe('Missing Access Token');
  });

});
