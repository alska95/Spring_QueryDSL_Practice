package com.example.querydsl.repository;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;

import java.util.List;

/**
 * memberRepositoryCustom에 대한 구현체는 MemberRepository가 못만들어 주니까,
 * 우리가 MemberRepositoryImpl로 구현해줘야 한다.
 * 이름을 맞춰주는 이유는 MemberRepository가 찾아서 넣기 위해서
 * */
public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition condition);
}
