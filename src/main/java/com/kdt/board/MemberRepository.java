package com.kdt.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/*
 * [영속화] Repository = DB 접근 창구 (2단계)
 *
 * JpaRepository<Member, Long>
 * - Member : 영속화 대상 Entity
 * - Long   : PK 타입 (Member.id)
 *
 * Spring Data JPA가 구현체를 자동 생성 → Bean으로 등록
 * 주요 메서드:
 * - save(entity)   : INSERT/UPDATE를 하나로 처리, 저장 후 id 채워짐
 * - findAll()      : DB에서 조회 → Member 리스트 (영속 상태)
 * - findById(id)   : PK로 단건 조회 → Optional<Member>
 * - deleteById(id) : 삭제
 *
 * ★ 왜 insert() / update()가 없고 save()만 있나? (JPA 디자이너에도 안 보임)
 * - SQL은 INSERT/UPDATE를 나누지만, JPA는 "저장(save)" 중심
 * - 메서드 이름이 아니라 엔티티 상태로 자동 분기함
 *   · id == null (비영속) → INSERT
 *   · id 있음 (이미 DB 행) → UPDATE
 * - 그래서 Repository/디자이너에 insert·update 메서드가 따로 없음
 *
 * 순서상 Service가 이 Repository를 호출해서 실제 영속화를 수행함
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByNickname(String nickname);

    Optional<Member> findByLoginId(String loginId);

}
