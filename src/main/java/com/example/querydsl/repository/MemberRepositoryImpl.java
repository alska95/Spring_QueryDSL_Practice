package com.example.querydsl.repository;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;

import com.example.querydsl.dto.QMemberTeamDto;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.QTeam;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.querydsl.entity.QMember.*;
import static com.example.querydsl.entity.QTeam.*;

public class MemberRepositoryImpl implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory.select(new QMemberTeamDto(member.id.as("memberId"), member.name , member.age , team.id.as("teamId") , team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team(), team)
                .where(
                        ageGoe(condition.getAgeGoe())
                        ,ageLoe(condition.getAgeLoe())
                        ,nameEq(condition.getName())
                        ,teamNameEq(condition.getTeamName())
                )
                .fetch();
    }

    public BooleanExpression ageGoe(Integer ageGoe){
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }
    public BooleanExpression ageLoe(Integer ageLoe){
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
    public BooleanExpression nameEq(String name){
        return StringUtils.hasText(name) ? member.name.eq(name) : null;
    }
    public BooleanExpression teamNameEq(String teamName){
        return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
    }

}
