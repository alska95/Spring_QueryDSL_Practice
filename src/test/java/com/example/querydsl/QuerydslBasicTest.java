package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.QTeam;
import com.example.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
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
import static com.example.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;

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

        assertThat(findMember.getName()).isEqualTo("member1");

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
        assertThat(findMember.getName()).isEqualTo("member1");
    }


    @Test
    public void QTypes(){
        queryFactory = new JPAQueryFactory(em) ;
        QMember m = new QMember("m");
        QMember m2 = member;

        Member findMember = queryFactory
                .select(member) //static으로 호출할수도 있음.
                .from(member)
                .where(member.name.eq("member1")) //파라미터 바인딩 안해줘도 댐. prepare statement로 알아서 해준다.
                .fetchOne();
        assertThat(findMember.getName()).isEqualTo("member1");
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
        assertThat(findMember.getName()).isEqualTo("member1");

    }

    @Test
    public void getResult(){
        queryFactory = new JPAQueryFactory(em) ;
        List<Member> fetch = queryFactory.selectFrom(member).fetch();//list 조회
        Member member0 = queryFactory.selectFrom(QMember.member).fetchOne();//단건 조회
        Member member1 = queryFactory.selectFrom(QMember.member).fetchFirst();//첫번째 것만 조회. == .limit(1).fetchOne();
        QueryResults<Member> results = queryFactory.selectFrom(QMember.member).fetchResults(); //QueryResult형으로 반환, result로 여러 조작을 할 수 있다.
        results.getTotal(); //쿼리가 한번 더 실행된다. 컨텐츠를 가져오는 쿼리와 count가져오는 쿼리가 다를 경우 (성능때문에) 사용하지 않는 것이 좋다.
        results.getResults();

        long total = queryFactory.selectFrom(member).fetchCount();

    }
    
    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 단 회원 이름이 없으면 마지막에 출력(nulls last)*/
    @Test
    public void sort(){
        queryFactory = new JPAQueryFactory(em);
        em.persist(new Member(null, 100));
        em.persist(new Member("member5" , 90));
        em.persist(new Member("member6" , 90));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.gt(1))
                .orderBy(member.age.desc(), member.name.asc().nullsLast())
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1.getName() = " + member1.getName());
            System.out.println("member1.getAge() = " + member1.getAge());
        }
    }

    @Test
    public void pageing(){
        queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory.selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);

        QueryResults<Member> results = queryFactory.selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(results.getTotal()).isEqualTo(4); //count쿼리가 어떻게 다른가? 알아보기
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getResults().size()).isEqualTo(2);

    }

    @Test
    public void aggregation(){
        queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory.select(
                member.count()
                , member.age.sum()
                , member.age.avg()
                , member.age.max()
                , member.age.min()
        )
                .from(member)
                .fetch(); //집합은 튜플로 조회하게된다.
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        System.out.println("member.age.avg() = " + member.age.avg());
        System.out.println("member.age.max() = " + member.age.max());
        System.out.println("member.age.min() = " + member.age.min());

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     * */
    @Test
    public void group(){
        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team(), team)
                .groupBy(team.name)
                .having(member.age.gt(0))
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
    }

}
