package com.kdt.board;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** @Service Bean — HelloController 에 생성자 주입 */
@Service
public class HelloService {

    public String message() {
        return "kimbap";
    }

    public List<String> message2(String a, String b) {
        List<String> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        return list;
    }
}
