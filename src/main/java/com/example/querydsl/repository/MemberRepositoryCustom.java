package com.example.querydsl.repository;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * memberRepositoryCustom에 대한 구현체는 MemberRepository가 못만들어 주니까,
 * 우리가 MemberRepositoryImpl로 구현해줘야 한다.
 * 이름을 맞춰주는 이유는 MemberRepository가 찾아서 넣기 위해서
 * */
public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition condition);
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition , Pageable pageable);
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
