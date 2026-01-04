package com.zerotrust.identity.service;


import com.zerotrust.identity.entertity.TestIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zerotrust.common.Response.Result;


@Service
public class TestIdentityService {


    public Result test01() {
      TestIdentity testIdentity=new TestIdentity();
      testIdentity.setId(1);
      testIdentity.setRemark("正在测试identity模块...");
        System.out.println("到哪了2");
      return  Result.success(testIdentity);
    }
}
