package com.kb.tangtang.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Log4j2 // 콘솔에 내가 원한느 메세지로 출력 가능
public class HomeController {

    // 어떤 주소로(/), 어떤 방법으로(get)
    // 하나의 http 요청 당 서버에서는 메서드 하나랑 정의

    @GetMapping("/")
    public String home() {
        log.info("================> HomController /");
        return "home"; // View의 이름
    }
}
