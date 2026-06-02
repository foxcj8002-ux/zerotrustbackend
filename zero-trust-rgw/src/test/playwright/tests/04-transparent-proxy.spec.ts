import { test, expect } from '@playwright/test';
import { generateTestJwt, TEST_RESOURCES, TEST_USERS } from '../utils/fixtures';

/**
 * Test Suite: 04-transparent-proxy.spec.ts
 * 测试目标: 验证透明代理功能
 * - TC12: 成功代理到百度首页
 * - TC13: 成功代理到 JSONPlaceholder API
 * - TC14: 子路径转发正确
 * - TC15: Query 参数转发正确
 * 
 * 前提条件:
 * 1. JWT Token 有效
 * 2. Redis 中存在对应 sessionId 的策略
 * 3. MySQL 中存在对应的资源映射
 * 4. 目标主机在白名单中
 */

test.describe('透明代理测试套件 - Transparent Proxy Tests', () => {

  /**
   * TC12: 成功代理到公网 - 百度首页
   * 验证完整认证链: Token + Session + 策略 + 资源在白名单
   */
  test('TC12: 成功代理到百度首页 - 应该返回 200 并包含百度内容', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: TEST_USERS.user001.userId,
      resourceId: TEST_RESOURCES.baidu.resourceId,
    });

    // 先通过 API 设置策略
    const policyResponse = await request.post('/gateway/policy/receive', {
      data: {
        sessionId: TEST_USERS.user001.sessionId,
        userId: TEST_USERS.user001.userId,
        resourceId: TEST_RESOURCES.baidu.resourceId,
        action: 'GET',
        allowed: true,
        expireTime: Date.now() + 3600000, // 1小时后过期
      },
    });
    expect(policyResponse.ok()).toBeTruthy();

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

    const response = await page.goto(`/res/${TEST_RESOURCES.baidu.resourceId}/`);
    
    expect(response?.status()).toBe(200);
    const title = await page.title();
    expect(title).toMatch(/百度|Baidu/);
  });

  /**
   * TC13: 成功代理到 JSONPlaceholder API
   */
  test('TC13: 成功代理到 JSONPlaceholder API - 应该返回正确的 JSON 数据', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: TEST_USERS.user002.userId,
      resourceId: TEST_RESOURCES.jsonplaceholder.resourceId,
    });

    await request.post('/gateway/policy/receive', {
      data: {
        sessionId: TEST_USERS.user002.sessionId,
        userId: TEST_USERS.user002.userId,
        resourceId: TEST_RESOURCES.jsonplaceholder.resourceId,
        action: 'GET',
        allowed: true,
        expireTime: Date.now() + 3600000,
      },
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
      'X-Session-Id': TEST_USERS.user002.sessionId,
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.jsonplaceholder.resourceId}/posts/1`);
    
    expect(response?.status()).toBe(200);
    
    const contentType = response?.headers()['content-type'];
    expect(contentType).toContain('application/json');
    
    const bodyText = await page.evaluate(() => document.body.textContent || '');
    const data = JSON.parse(bodyText);
    expect(data.id).toBe(1);
    expect(data.userId).toBe(1);
  });

  /**
   * TC14: 子路径转发正确
   * 验证: /res/jsonplaceholder-1/users/2 -> jsonplaceholder.typicode.com/users/2
   */
  test('TC14: 子路径转发正确 - 应该返回正确的用户数据', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: TEST_USERS.user002.userId,
      resourceId: TEST_RESOURCES.jsonplaceholder.resourceId,
    });

    await request.post('/gateway/policy/receive', {
      data: {
        sessionId: 'session-path-test',
        userId: TEST_USERS.user002.userId,
        resourceId: TEST_RESOURCES.jsonplaceholder.resourceId,
        action: 'GET',
        allowed: true,
        expireTime: Date.now() + 3600000,
      },
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
      'X-Session-Id': 'session-path-test',
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.jsonplaceholder.resourceId}/users/2`);
    
    expect(response?.status()).toBe(200);
    
    const bodyText = await page.evaluate(() => document.body.textContent || '');
    const data = JSON.parse(bodyText);
    expect(data.id).toBe(2);
    // JSONPlaceholder uses 'name' not 'first_name'
    expect(data.name).toBeTruthy();
  });

  /**
   * TC15: Query 参数转发正确
   */
  test('TC15: Query 参数转发正确 - 应该只返回匹配的数据', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: TEST_USERS.user002.userId,
      resourceId: TEST_RESOURCES.jsonplaceholder.resourceId,
    });

    await request.post('/gateway/policy/receive', {
      data: {
        sessionId: 'session-query-test',
        userId: TEST_USERS.user002.userId,
        resourceId: TEST_RESOURCES.jsonplaceholder.resourceId,
        action: 'GET',
        allowed: true,
        expireTime: Date.now() + 3600000,
      },
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
      'X-Session-Id': 'session-query-test',
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.jsonplaceholder.resourceId}/posts?userId=1`);
    
    expect(response?.status()).toBe(200);
    
    const bodyText = await page.evaluate(() => document.body.textContent || '');
    const data = JSON.parse(bodyText);
    
    expect(Array.isArray(data)).toBeTruthy();
    expect(data.length).toBeGreaterThan(0);
    expect(data.every((item: any) => item.userId === 1)).toBeTruthy();
  });

  /**
   * TC12-B: JSONPlaceholder API 扩展测试 - Photos 端点
   * 由于 ReqRes.in 被 Cloudflare 阻止，改用 JSONPlaceholder 测试另一个子路径
   */
  test('TC12-B: JSONPlaceholder Photos API - 应该返回照片数据', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: TEST_USERS.user002.userId,
      resourceId: TEST_RESOURCES.jsonplaceholder.resourceId,
    });

    await request.post('/gateway/policy/receive', {
      data: {
        sessionId: 'session-photos-test',
        userId: TEST_USERS.user002.userId,
        resourceId: TEST_RESOURCES.jsonplaceholder.resourceId,
        action: 'GET',
        allowed: true,
        expireTime: Date.now() + 3600000,
      },
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
      'X-Session-Id': 'session-photos-test',
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.jsonplaceholder.resourceId}/photos/1`);
    
    expect(response?.status()).toBe(200);
    
    const bodyText = await page.evaluate(() => document.body.textContent || '');
    const data = JSON.parse(bodyText);
    expect(data.id).toBe(1);
    expect(data.albumId).toBe(1);
  });

});
