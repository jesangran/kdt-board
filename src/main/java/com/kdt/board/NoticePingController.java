package com.kdt.board;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoticePingController {

    @GetMapping("/notice") //데이터를 가져올(조회할) 때 쓰는 매핑
    public String notice(){
        return "closed";
    }
}
