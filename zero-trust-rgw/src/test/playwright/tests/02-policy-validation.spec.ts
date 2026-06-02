import { test, expect } from '@playwright/test';
import { generateTestJwt, TEST_RESOURCES, TEST_USERS } from '../utils/fixtures';

/**
 * Test Suite: 02-policy-validation.spec.ts
 * 测试目标: 验证策略校验机制 (Redis 中的策略验证)
 * - TC04: 策略未下发
 * - TC05: 用户 ID 不匹配
 * - TC06: 资源 ID 不匹配
 * - TC07: 策略过期
 */

test.describe('策略校验测试套件 - Policy Validation Tests', () => {

  /**
   * TC04: 策略未下发 - 访问被拒绝
   * 条件: 有效 Token + 不存在的 Session
   */
  test('TC04: 策略未下发 - 应该返回 403 Forbidden', async ({ context, page }) => {
    const validJwt = await generateTestJwt({
      sub: TEST_USERS.user001.userId,
      resourceId: TEST_USERS.user001.resourceId,
    });

    await context.addCookies([{
      name: 'access_token',
      value: validJwt,
      domain: 'localhost',
      path: '/',
      secure: true,
      httpOnly: false,
    }]);

    await page.setExtraHTTPHeaders({
      'X-Session-Id': 'nonexistent-session-id-12345',
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.baidu.resourceId}/`);
    
    expect(response?.status()).toBe(403);
    expect(response?.headers()['x-error-message']).toContain('Access Denied');
  });

  /**
   * TC05: 用户 ID 不匹配 - 访问被拒绝
   * 条件: Token 中 userId=A, 策略中 userId=B
   * 注意: 需要预先在 Redis 中设置 user-002 的策略, 然后用 user-001 的 Token 访问
   */
  test('TC05: 用户 ID 不匹配 - 应该返回 403 Forbidden', async ({ context, page }) => {
    // 使用 user-001 的 Token, 但请求的资源配置的是 user-002 的策略
    const validJwt = await generateTestJwt({
      sub: 'user-001',
      resourceId: TEST_RESOURCES.baidu.resourceId,
    });

    await context.addCookies([{
      name: 'access_token',
      value: validJwt,
      domain: 'localhost',
      path: '/',
      secure: true,
      httpOnly: false,
    }]);

    // 假设 Redis 中只有 session-002 (属于 user-002) 的策略
    await page.setExtraHTTPHeaders({
      'X-Session-Id': TEST_USERS.user002.sessionId,
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.baidu.resourceId}/`);
    
    expect(response?.status()).toBe(403);
  });

  /**
   * TC06: 资源 ID 不匹配 - 访问被拒绝
   * 条件: Token 中 resourceId=A, 请求 resourceId=B
   */
  test('TC06: 资源 ID 不匹配 - 应该返回 403 Forbidden', async ({ context, page }) => {
    // Token 中指定的是 baidu-test, 但请求的是 jsonplaceholder-1
    const validJwt = await generateTestJwt({
      sub: TEST_USERS.user001.userId,
      resourceId: 'baidu-test',  // Token 中指定 baidu-test
    });

    await context.addCookies([{
      name: 'access_token',
      value: validJwt,
      domain: 'localhost',
      path: '/',
      secure: true,
      httpOnly: false,
    }]);

    await page.setExtraHTTPHeaders({
      'X-Session-Id': TEST_USERS.user001.sessionId,
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.jsonplaceholder.resourceId}/posts`);
    
    expect(response?.status()).toBe(403);
    expect(response?.headers()['x-error-message']).toBe('Resource ID Mismatch');
  });

  /**
   * TC07: 策略过期 - 访问被拒绝
   * 条件: Redis 中的策略 expireTime < 当前时间
   * 注意: 需要通过 /gateway/policy/receive 设置一个已过期的策略
   */
  test('TC07: 策略过期 - 应该返回 403 Forbidden', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: 'expired-test-user',
      resourceId: TEST_RESOURCES.baidu.resourceId,
    });

    // 创建一个已过期的策略
    const expiredPolicy = {
      sessionId: 'expired-session-test',
      userId: 'expired-test-user',
      resourceId: TEST_RESOURCES.baidu.resourceId,
      action: 'GET',
      allowed: true,
      expireTime: Date.now() - 3600000, // 1小时前过期
    };

    await request.post('/gateway/policy/receive', { data: expiredPolicy });

    await context.addCookies([{
      name: 'access_token',
      value: validJwt,
      domain: 'localhost',
      path: '/',
      secure: true,
      httpOnly: false,
    }]);

    await page.setExtraHTTPHeaders({
      'X-Session-Id': 'expired-session-test',
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.baidu.resourceId}/`);
    
    expect(response?.status()).toBe(403);
  });

  /**
   * TC04-B: 缺少 X-Session-Id Header
   */
  test('TC04-B: 缺少 Session ID - 应该返回 403', async ({ context, page }) => {
    const validJwt = await generateTestJwt({
      sub: TEST_USERS.user001.userId,
      resourceId: TEST_USERS.user001.resourceId,
    });

    await context.addCookies([{
      name: 'access_token',
      value: validJwt,
      domain: 'localhost',
      path: '/',
      secure: true,
      httpOnly: false,
    }]);

    // 不设置 X-Session-Id header

    const response = await page.goto(`/res/${TEST_RESOURCES.baidu.resourceId}/`);
    
    expect(response?.status()).toBe(403);
    expect(response?.headers()['x-error-message']).toBe('Missing Session ID');
  });

});
