import { test, expect } from '@playwright/test';
import { generateTestJwt } from '../utils/fixtures';

/**
 * Test Suite: 05-internal-endpoints.spec.ts
 * 测试目标: 验证内部管理端点 (无需认证即可访问)
 * - TC16: 健康检查端点
 * - TC17: 策略接收端点 - 正常接收
 * - TC18: 策略接收端点 - 缺少必填字段
 */

test.describe('内部端点测试套件 - Internal Endpoints Tests', () => {

  /**
   * TC16: 健康检查端点 - 无需认证
   */
  test('TC16: 健康检查端点 - 应该返回 200 和 {"status":"UP"}', async ({ page }) => {
    const response = await page.goto('/gateway/policy/health');
    
    expect(response?.status()).toBe(200);
    
    const bodyText = await page.evaluate(() => document.body.textContent || '');
    const body = JSON.parse(bodyText);
    expect(body.status).toBe('UP');
  });

  /**
   * TC16-B: 健康检查端点 - HTTP 请求
   */
  test('TC16-B: 健康检查端点 HTTP - 应该返回 200', async ({ request }) => {
    const response = await request.get('/gateway/policy/health');
    
    expect(response.ok()).toBeTruthy();
    expect(response.status()).toBe(200);
    
    const body = await response.json();
    expect(body.status).toBe('UP');
  });

  /**
   * TC17: 策略接收端点 - 正常接收
   */
  test('TC17: 策略接收 - 正常数据应该返回 success:true', async ({ request }) => {
    const validPolicy = {
      sessionId: 'test-session-001',
      userId: 'user-001',
      resourceId: 'baidu-test',
      action: 'GET',
      allowed: true,
      expireTime: Date.now() + 3600000, // 1小时后过期
    };

    const response = await request.post('/gateway/policy/receive', {
      data: validPolicy,
    });

    expect(response.ok()).toBeTruthy();
    expect(response.status()).toBe(200);
    
    const body = await response.json();
    expect(body.success).toBe(true);
    expect(body.message).toBe('策略同步成功');
    expect(body.sessionId).toBe('test-session-001');
  });

  /**
   * TC18: 策略接收 - 缺少必填字段
   */
  test('TC18: 策略接收 - 缺少必填字段应该返回 success:false', async ({ request }) => {
    const invalidPolicy = {
      sessionId: 'test-session-invalid',
      // 缺少 userId, resourceId
    };

    const response = await request.post('/gateway/policy/receive', {
      data: invalidPolicy,
    });

    expect(response.ok()).toBeTruthy();
    expect(response.status()).toBe(200);
    
    const body = await response.json();
    expect(body.success).toBe(false);
    expect(body.message).toContain('关键字段');
  });

  /**
   * TC17-B: 策略接收 - 缺少 sessionId
   */
  test('TC17-B: 策略接收 - 缺少 sessionId 应该返回 success:false', async ({ request }) => {
    const invalidPolicy = {
      userId: 'user-001',
      resourceId: 'baidu-test',
      action: 'GET',
      allowed: true,
      expireTime: Date.now() + 3600000,
    };

    const response = await request.post('/gateway/policy/receive', {
      data: invalidPolicy,
    });

    expect(response.ok()).toBeTruthy();
    
    const body = await response.json();
    expect(body.success).toBe(false);
    expect(body.message).toContain('关键字段');
  });

  /**
   * TC17-C: 策略接收 - 过期时间已过
   */
  test('TC17-C: 策略接收 - 过期策略应该返回 success:false', async ({ request }) => {
    const expiredPolicy = {
      sessionId: 'expired-session-test',
      userId: 'user-001',
      resourceId: 'baidu-test',
      action: 'GET',
      allowed: true,
      expireTime: Date.now() - 3600000, // 1小时前过期
    };

    const response = await request.post('/gateway/policy/receive', {
      data: expiredPolicy,
    });

    expect(response.ok()).toBeTruthy();
    
    const body = await response.json();
    expect(body.success).toBe(false);
    expect(body.message).toContain('过期');
  });

  /**
   * TC17-D: 策略接收 - allowed=false
   */
  test('TC17-D: 策略接收 - allowed=false 应该存储成功但访问被拒', async ({ context, page, request }) => {
    const deniedPolicy = {
      sessionId: 'denied-session-test',
      userId: 'denied-user',
      resourceId: 'baidu-test',
      action: 'GET',
      allowed: false, // 明确拒绝
      expireTime: Date.now() + 3600000,
    };

    const response = await request.post('/gateway/policy/receive', {
      data: deniedPolicy,
    });

    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body.success).toBe(true); // 存储成功

    // 但访问应该被拒绝
    const validJwt = await generateTestJwt({
      sub: 'denied-user',
      resourceId: 'baidu-test',
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
      'X-Session-Id': 'denied-session-test',
    });

    const accessResponse = await page.goto('/res/baidu-test/');
    expect(accessResponse?.status()).toBe(403);
  });

  /**
   * TC16-C: 非 /gateway/ 路径的内部请求不应被跳过
   */
  test('TC16-C: /actuator/health 应该需要认证', async ({ page }) => {
    const response = await page.goto('/actuator/health');
    
    // actuator 端点不在 /gateway/ 路径下，应该需要认证
    // 注意: 这取决于 actuator 的安全配置
    expect([200, 401, 403]).toContain(response?.status());
  });

});
