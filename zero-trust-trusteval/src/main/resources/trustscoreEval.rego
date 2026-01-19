package my.policyrego


default trustscore := 100

# === 规则定义 (Rules) ===
# 使用 violations 集合来收集所有的违规项
# 格式: {"score": 扣分值, "reason": "扣分原因"}

# 1. 网络类型检查
# 如果不是 corporate (企业内网)，视为高风险环境，扣 20 分
violations[{"score": 20, "reason": "Non-corporate network detected"}] {
    input.networkType != "corporate"
}

# 2. TLS 版本检查
# 必须严格使用 TLS1.3，否则扣 10 分
violations[{"score": 10, "reason": "Weak TLS version"}] {
    input.tlsVersion != "TLS1.3"
}

# 3. 国家地理围栏 (Geo-fencing)
# 如果 IP 不在中国 (CN)，直接视为高危，扣 50 分
violations[{"score": 50, "reason": "Access from unauthorized country"}] {
    input.country != "CN"
}

# 4. 城市白名单
# 即使在中国，如果不在核心办公城市，扣 15 分
violations[{"score": 15, "reason": "Access from non-standard city"}] {
    # 定义允许的城市列表
    allowed_cities := {"Beijing", "Shanghai", "Shenzhen", "Hangzhou"}
    # 判断 input 城市是否在列表中
    not allowed_cities[input.city]
}

# 5. 操作频率 (高频访问检测)
# 10分钟内操作超过 500 次，视为潜在爬虫或脚本，扣 25 分
violations[{"score": 25, "reason": "Abnormal operation frequency"}] {
    input.ops10m > 500
}

# 6. 数据传出量 (DLP - Data Loss Prevention)
# 10分钟内传出流量超过 50MB，视为数据泄露风险，扣 30 分
violations[{"score": 30, "reason": "High data egress detected"}] {
    # 50MB = 50 * 1024 * 1024 = 52428800 bytes
    limit := 52428800
    input.bytesOut10m > limit
}

# === 结果聚合 ===

# 计算总扣分
total_penalty := sum([item.score | item := violations[_]])

# 计算最终得分
trustscore := max([0, 100 - total_penalty])
# 最终返回的 JSON 结构

result := {
    "trustscore": trustscore
}
