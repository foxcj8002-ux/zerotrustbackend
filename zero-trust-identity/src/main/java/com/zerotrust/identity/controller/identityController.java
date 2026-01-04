package com.zerotrust.identity.controller;


import com.zerotrust.identity.entertity.TestIdentity;
import com.zerotrust.identity.service.TestIdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zerotrust.common.Response.Result;

@RestController

public class identityController {

    @Autowired
    private TestIdentityService testIdentityService;

    @GetMapping ("/identity/testidentity")
    public Result test01(){
        System.out.println("到哪了1");
        return testIdentityService.test01();
    }


}
