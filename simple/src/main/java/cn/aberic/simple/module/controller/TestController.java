package cn.aberic.simple.module.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 胡柏吉
 * @version 1.0
 * @description TODO
 * @date 2020-09-15 下午7:40
 * @email drbjhu@163.com
 */

@RestController
public class TestController {
    @RequestMapping("/hello")
    public String index(){
        return "Hello World， Spring boot is good";
    }
}
