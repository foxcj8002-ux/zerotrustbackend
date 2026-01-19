package com.zerotrust.trusteval.controller;

import com.zerotrust.trusteval.service.OpaService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/trusteval/opa")
public class OpaController {

    private final OpaService opaService;

    public OpaController(OpaService opaService) {
        this.opaService = opaService;
    }

   //将输入的inputdata执行后返回结果
    @PostMapping("/evaluate")
    public Map<String, Object> evaluate(@RequestBody Map<String, Object> inputData) {
        Map<String, Object> result = opaService.executePolicy(inputData);
        return result;
    }
}