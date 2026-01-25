package com.zerotrust.policy.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zerotrust.common.Response.Result;

import java.util.Map;

@RestController
@RequestMapping("/policy/resources")
public class ResourcesController {
    @GetMapping("/{id}")
    public Object rgwtest(@PathVariable("id") Long id){
        System.out.println("HIT ResourcesController id=" + id);
        return Map.of("msg", "这是资源" + id);
    }
}
