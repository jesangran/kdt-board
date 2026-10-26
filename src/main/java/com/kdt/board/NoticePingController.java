package com.kdt.board;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoticePingController {

    @GetMapping("/notice")
    public String notice() {
        return "closed";
    }
}
