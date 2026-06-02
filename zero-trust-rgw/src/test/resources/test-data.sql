-- Zero-Trust Gateway 测试数据初始化脚本
-- 用途: 为 Playwright E2E 测试准备 MySQL 数据

USE zerotrust_rgw;

-- 清空现有测试数据 (如果需要)
-- DELETE FROM resource WHERE resource_id LIKE 'ssrf-%';

-- 插入测试用资源
INSERT INTO resource (resource_id, resource_name, resource_url, resource_type) VALUES
-- 正常代理测试资源
('baidu-test', '百度搜索', 'https://www.baidu.com', 'search'),
('jsonplaceholder-1', 'JSONPlaceholder API', 'https://jsonplaceholder.typicode.com', 'api'),
('reqres-1', 'ReqRes API', 'https://reqres.in', 'api'),

-- SSRF 测试资源 (这些资源的 URL 指向内网, 应该被拦截)
('ssrf-localhost', 'SSRF Test Localhost', 'http://127.0.0.1:8080', 'test'),
('ssrf-private-10', 'SSRF Test 10.x', 'http://10.0.0.1/admin', 'test'),
('ssrf-private-192', 'SSRF Test 192.168.x', 'http://192.168.1.100/api', 'test'),
('ssrf-private-172', 'SSRF Test 172.16-31.x', 'http://172.20.0.1:8080', 'test'),
('ssrf-metadata', 'SSRF Test Metadata', 'http://169.254.169.254/latest/meta-data/', 'test'),
('ssrf-localhost-name', 'SSRF Test Localhost Name', 'http://localhost:8080', 'test'),

-- 其他测试资源
('httpbin-test', 'HTTPBin Test', 'https://httpbin.org', 'api')
ON DUPLICATE KEY UPDATE
  resource_name = VALUES(resource_name),
  resource_url = VALUES(resource_url),
  resource_type = VALUES(resource_type);

-- 验证插入结果
SELECT * FROM resource WHERE resource_id LIKE '%test%' OR resource_id LIKE '%ssrf%';
