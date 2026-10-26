package com.kdt.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/*
 * [영속화] MemberRepository — JpaRepository (Member, Long)
 * save = INSERT/UPDATE 자동 분기 (PostRepository 주석과 동일)
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByNickname(String nickname);

    Optional<Member> findByLoginId(String loginId);
}
