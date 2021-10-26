package com.example.querydsl.repository;

import com.example.querydsl.entity.Member;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJPARepositoryTest {
    @PersistenceContext
    EntityManager em;

    @Autowired MemberJPARepository memberJPARepository;

    @Test
    public void basicTest(){
        Member member = new Member("member1", 10);
        memberJPARepository.save(member);

        Member findMember = memberJPARepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> all = memberJPARepository.findAll();
        assertThat(all.size()).isEqualTo(1);

        List<Member> member1 = memberJPARepository.findByName("member1");
        assertThat(member1).containsExactly(member);
    }

    @Test
    public void basicQueryDslTest(){
        Member member = new Member("member1", 10);
        memberJPARepository.save(member);

        Member findMember = memberJPARepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> all = memberJPARepository.findAll_Querydsl();
        assertThat(all.size()).isEqualTo(1);

        List<Member> member1 = memberJPARepository.findByName_Querydsl("member1");
        assertThat(member1).containsExactly(member);
    }
}