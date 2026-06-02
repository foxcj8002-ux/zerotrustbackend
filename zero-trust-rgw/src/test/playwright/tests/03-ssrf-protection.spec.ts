import { test, expect } from '@playwright/test';
import { generateTestJwt, TEST_RESOURCES } from '../utils/fixtures';

/**
 * Test Suite: 03-ssrf-protection.spec.ts
 * 测试目标: 验证 SSRF 安全防护机制
 * - TC08: 127.0.0.1 拦截
 * - TC09: 10.x.x.x 拦截
 * - TC10: 192.168.x.x 拦截
 * - TC11: 169.254.x.x (云元数据) 拦截
 * 
 * SSRF 防护逻辑:
 * 1. 黑名单: loopback (127.0.0.0/8), link-local (169.254.0.0/16), RFC1918 (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
 * 2. 白名单: gateway.safety.allowed-hosts 配置的主机
 */

test.describe('SSRF 防护测试套件 - SSRF Protection Tests', () => {

  /**
   * TC08: 内网 IP 拦截 - 127.0.0.1
   * 该资源配置的 URL 指向 127.0.0.1:8080，不在白名单中
   */
  test('TC08: 127.0.0.1 拦截 - 应该返回 403 Forbidden', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: 'ssrf-test-user',
      resourceId: TEST_RESOURCES.ssrf_localhost.resourceId,
    });

    // 先设置有效的策略（必须先通过策略检查才能到达 SSRF 检查）
    await request.post('/gateway/policy/receive', {
      data: {
        sessionId: 'ssrf-test-session-localhost',
        userId: 'ssrf-test-user',
        resourceId: TEST_RESOURCES.ssrf_localhost.resourceId,
        action: 'GET',
        allowed: true,
        expireTime: Date.now() + 3600000,
      }
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
      'X-Session-Id': 'ssrf-test-session-localhost',
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.ssrf_localhost.resourceId}/`);
    
    expect(response?.status()).toBe(403);
    expect(response?.headers()['x-error-message']).toBe('Access Denied');
  });

  /**
   * TC09: 内网 IP 拦截 - 10.x.x.x
   */
  test('TC09: 10.x.x.x 内网 IP 拦截 - 应该返回 403 Forbidden', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: 'ssrf-test-user',
      resourceId: TEST_RESOURCES.ssrf_private_10.resourceId,
    });

    // 先设置有效的策略
    await request.post('/gateway/policy/receive', {
      data: {
        sessionId: 'ssrf-test-session-10',
        userId: 'ssrf-test-user',
        resourceId: TEST_RESOURCES.ssrf_private_10.resourceId,
        action: 'GET',
        allowed: true,
        expireTime: Date.now() + 3600000,
      }
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
      'X-Session-Id': 'ssrf-test-session-10',
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.ssrf_private_10.resourceId}/admin`);
    
    expect(response?.status()).toBe(403);
    expect(response?.headers()['x-error-message']).toBe('Access Denied');
  });

  /**
   * TC10: 内网 IP 拦截 - 192.168.x.x
   */
  test('TC10: 192.168.x.x 内网 IP 拦截 - 应该返回 403 Forbidden', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: 'ssrf-test-user',
      resourceId: TEST_RESOURCES.ssrf_private_192.resourceId,
    });

    // 先设置有效的策略
    await request.post('/gateway/policy/receive', {
      data: {
        sessionId: 'ssrf-test-session-192',
        userId: 'ssrf-test-user',
        resourceId: TEST_RESOURCES.ssrf_private_192.resourceId,
        action: 'GET',
        allowed: true,
        expireTime: Date.now() + 3600000,
      }
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
      'X-Session-Id': 'ssrf-test-session-192',
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.ssrf_private_192.resourceId}/api`);
    
    expect(response?.status()).toBe(403);
    expect(response?.headers()['x-error-message']).toBe('Access Denied');
  });

  /**
   * TC11: 内网 IP 拦截 - 169.254.x.x (云元数据地址)
   * 这是最危险的 SSRF 攻击向量，可获取云实例元数据
   */
  test('TC11: 169.254.x.x 元数据地址拦截 - 应该返回 403 Forbidden', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: 'ssrf-test-user',
      resourceId: TEST_RESOURCES.ssrf_metadata.resourceId,
    });

    // 先设置有效的策略
    await request.post('/gateway/policy/receive', {
      data: {
        sessionId: 'ssrf-test-session-metadata',
        userId: 'ssrf-test-user',
        resourceId: TEST_RESOURCES.ssrf_metadata.resourceId,
        action: 'GET',
        allowed: true,
        expireTime: Date.now() + 3600000,
      }
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
      'X-Session-Id': 'ssrf-test-session-metadata',
    });

    const response = await page.goto(`/res/${TEST_RESOURCES.ssrf_metadata.resourceId}/latest/meta-data/`);
    
    expect(response?.status()).toBe(403);
    expect(response?.headers()['x-error-message']).toBe('Access Denied');
  });

  /**
   * TC11-B: 172.16.x.x - 172.31.x.x 私网地址拦截
   */
  test('TC11-B: 172.16-31.x.x 内网 IP 拦截 - 应该返回 403', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: 'ssrf-test-user',
      resourceId: 'ssrf-private-172',
    });

    // 先设置有效的策略
    await request.post('/gateway/policy/receive', {
      data: {
        sessionId: 'ssrf-test-session-172',
        userId: 'ssrf-test-user',
        resourceId: 'ssrf-private-172',
        action: 'GET',
        allowed: true,
        expireTime: Date.now() + 3600000,
      }
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
      'X-Session-Id': 'ssrf-test-session-172',
    });

    // 资源 URL 为 http://172.20.0.1:8080 (不在白名单)
    const response = await page.goto('/res/ssrf-private-172/');
    
    expect(response?.status()).toBe(403);
    expect(response?.headers()['x-error-message']).toBe('Access Denied');
  });

  /**
   * TC08-B: localhost 域名拦截 (解析为 127.0.0.1)
   */
  test('TC08-B: localhost 域名拦截 - 应该返回 403', async ({ context, page, request }) => {
    const validJwt = await generateTestJwt({
      sub: 'ssrf-test-user',
      resourceId: 'ssrf-localhost-name',
    });

    // 先设置有效的策略
    await request.post('/gateway/policy/receive', {
      data: {
        sessionId: 'ssrf-test-session-localhost-name',
        userId: 'ssrf-test-user',
        resourceId: 'ssrf-localhost-name',
        action: 'GET',
        allowed: true,
        expireTime: Date.now() + 3600000,
      }
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
      'X-Session-Id': 'ssrf-test-session-localhost-name',
    });

    // 资源 URL 为 http://localhost:8080 (解析为 127.0.0.1)
    const response = await page.goto('/res/ssrf-localhost-name/');
    
    expect(response?.status()).toBe(403);
    expect(response?.headers()['x-error-message']).toBe('Access Denied');
  });

});
