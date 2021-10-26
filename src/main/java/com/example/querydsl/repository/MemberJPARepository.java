package com.example.querydsl.repository;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

import static com.example.querydsl.entity.QMember.*;

@Repository //DAO
//@AllArgsConstructor
public class MemberJPARepository {

    @PersistenceContext
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;


    /**
     * 싱글톤 객체를 모든 객체에서 공유해서 쓰는데 문제가 없나??
     * JPAQueryFactory의 동시성 문제는 EntityManager에 의존한다.
     * EntityManager은 동시성 문제와 상관 없이 트랜잭션 단위로 따로따로 분리되어 동작하게 된다.
     * EntityManager은 진짜 영속성 컨텍스트가 아니라, 프록시를 주입하고, 트랜잭션별로 다른대에 바인딩 되도록 라우팅만 해준다.
     *      --> 문제 없다!! 트랜잭션 범위의 영속성 컨텍스트를 참고할것.
     * */
//    public MemberJPARepository(EntityManager em) {
//        this.em = em;
//        this.queryFactory = new JPAQueryFactory(em);
//    }

    public MemberJPARepository(EntityManager em, JPAQueryFactory queryFactory) { //Bean으로 등록하고, 인젝션 받아서 사용하는것도 가능하다.
        this.em = em;
        this.queryFactory = queryFactory;
    }

    public void save(Member member){
        em.persist(member);
    }


    public Optional<Member> findById(Long id){
        Member findById = em.find(Member.class, id);
        return Optional.ofNullable(findById);
    }

    public List<Member> findAll(){
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }
    public List<Member> findAll_Querydsl(){
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByName(String name){
        return em.createQuery("select m from Member m where m.name =:name", Member.class)
                .getResultList();
    }
    public List<Member> findByName_Querydsl(String name){
        return queryFactory
                .selectFrom(member)
                .where(member.name.eq(name))
                .fetch();
    }
}
