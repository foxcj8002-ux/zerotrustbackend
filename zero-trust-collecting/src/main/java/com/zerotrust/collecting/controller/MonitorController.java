//package com.zerotrust.collecting.controller;
//
//import com.zerotrust.collecting.collector.ContextCollector;
//
//import javax.servlet.http.HttpServletRequest;
//
//@RestController
//@RequestMapping("/monitor")
//public class MonitorController {
//
//    @Autowired
//    private ContextCollector contextCollector;
//
//    @Autowired
//    private MonitoringService monitoringService;
//
//    @Autowired
//    private TrustEvalService trustEvalService;
//
//    @Autowired
//    private PolicyService policyService;
//
//    @PostMapping("/heartbeat")
//    public ResponseEntity<HeartbeatResponse> heartbeat(
//            @RequestBody HeartbeatRequest req,
//            HttpServletRequest request) {
//
//        // 1) 收集六大上下文
//        SecurityContext ctx = contextCollector.collect(req, request);
//
//        // 2) 监控事件构建
//        MonitoringEvent event = monitoringService.buildEvent(ctx);
//
//        // 3) 动态信任评估
//        TrustResult trustResult = trustEvalService.evaluate(ctx);
//
//        // 4) 策略引擎判定
//        PolicyDecision decision = policyService.decide(ctx, trustResult);
//
//        // 5) 返回心跳响应
//        HeartbeatResponse resp = new HeartbeatResponse(trustResult, decision);
//        return ResponseEntity.ok(resp);
//    }
//}
