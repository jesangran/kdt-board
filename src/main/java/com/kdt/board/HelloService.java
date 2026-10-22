package com.kdt.board;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/*
 * [Bean / Stereotype]
 *
 * Bean = Spring이 대신 만들어서 Container(창고)에 보관하는 객체
 * Stereotype = 역할 표시용 어노테이션 (@Service, @Controller, @Repository ...)
 *
 * @Service
 * - 비즈니스 로직을 담당하는 Bean/Component
 * - 이 클래스는 Container에 등록되고
 * - HelloController가 DI(생성자 주입)로 받아 사용함
 *
 * 개발자가 new HelloService() 하지 않아도,
 * Spring이 만들어 두고 필요할 때 꺼내 줌
 */
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
