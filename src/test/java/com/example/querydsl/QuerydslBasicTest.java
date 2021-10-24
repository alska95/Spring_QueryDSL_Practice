package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

import static com.example.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest{

    @Autowired
    EntityManager em;


    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10 , teamA);
        Member member2 = new Member("member2", 20 , teamA);
        Member member3 = new Member("member3", 10 , teamB);
        Member member4 = new Member("member4", 20 , teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJPQL(){
        //member1 찾기
        Member findMember = em.createQuery(
                "select m from Member m " +
                        "where m.name =:name", Member.class)
                .setParameter("name", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getName()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl(){
        queryFactory = new JPAQueryFactory(em) ; // 동시성 관리되게 처리되어 있다.
        // EntityManager자체가 multiThread환경에 문제없게 설계가 되어있다.
        // 트랜잭션이 어디 걸려있냐에 따라서 트랜젝션에 bound되게 설계되어있다.
        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.name.eq("member1")) //파라미터 바인딩 안해줘도 댐. prepare statement로 알아서 해준다.
                .fetchOne();
        Assertions.assertThat(findMember.getName()).isEqualTo("member1");
    }


    @Test
    public void QTypes(){
        queryFactory = new JPAQueryFactory(em) ;
        QMember m = new QMember("m");
        QMember m2 = QMember.member;

        Member findMember = queryFactory
                .select(member) //static으로 호출할수도 있음.
                .from(member)
                .where(member.name.eq("member1")) //파라미터 바인딩 안해줘도 댐. prepare statement로 알아서 해준다.
                .fetchOne();
        Assertions.assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    public void search(){
        queryFactory = new JPAQueryFactory(em) ;
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.name.eq("member1")
                        .and(member.age.eq(10))
                        .and(member.name.ne("abc"))
                        .and(member.name.eq("abc").not())
                        .and(member.age.in(10,20)) // 20 > >= 10
                        .and(member.age.gt(1)) // > 1
                        .and(member.age.goe(1))
                        .and(member.age.loe(100))
                        .and(member.age.lt(100))
                        .and(member.name.like("member%")) //like검색
                        .and(member.name.contains("member")) // %member%
                        ,member.name.startsWith("member") //,로 구분 지을경우 NULL을 무시해 주기 때문에 동적 쿼리를 매우 손쉽게 작성 가능하다.
                                )
                .fetchOne();
        Assertions.assertThat(findMember.getName()).isEqualTo("member1");

    }


}
