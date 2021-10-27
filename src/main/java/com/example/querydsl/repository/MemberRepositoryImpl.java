package com.example.querydsl.repository;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;

import com.example.querydsl.dto.QMemberTeamDto;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
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

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> result = queryFactory.select(new QMemberTeamDto(member.id.as("memberId"), member.name, member.age, team.id.as("teamId"), team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team(), team)
                .where(
                        ageGoe(condition.getAgeGoe())
                        , ageLoe(condition.getAgeLoe())
                        , nameEq(condition.getName())
                        , teamNameEq(condition.getTeamName())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();//fetchResult로 하면, count쿼리랑 content쿼리 둘다 날린다.

        return new PageImpl<>(result.getResults(),pageable , result.getTotal());

        /**
         * totalCount쓸때는 orderBy같은거 쓰지 말기!(count성능에 영향을 준다.)
         **/
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> result = queryFactory.select(
                new QMemberTeamDto(member.id.as("memberId"), member.name
                        , member.age, team.id.as("teamId")
                        , team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team(), team)
                .where(
                        ageGoe(condition.getAgeGoe())
                        , ageLoe(condition.getAgeLoe())
                        , nameEq(condition.getName())
                        , teamNameEq(condition.getTeamName())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();//fetchResult로 하면, count쿼리랑 content쿼리 둘다 날린다.

        JPAQuery<MemberTeamDto> countQuery = queryFactory.select(new QMemberTeamDto(
                member.id.as("memberId"), member.name
                , member.age, team.id.as("teamId")
                , team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team(), team)
                .where(
                        ageGoe(condition.getAgeGoe())
                        , ageLoe(condition.getAgeLoe())
                        , nameEq(condition.getName())
                        , teamNameEq(condition.getTeamName())
                );

        PageableExecutionUtils.getPage(result.getResults() , pageable , countQuery::fetchCount);
        //content와 pagable 토탈 사이즈를 보고 람다식을 생략할 수 있으면 생략한다.
        /**
         * 쿼리를 분류해서 count쿼리는 따로 날린다.
         * count쿼리를 왜 따로 짜지??
         * join이 필요 없는 등 count쿼리를 쉽게 짤 수 있는 경우가 많다. --> 성능의 차이를 야기함 (걍 select로 하면 조인 등등 다붙음)
         *
         * 카운트 쿼리를 생략할 수도 있다.
         *  페이지 시작하면서 컨텐츠 사이즈가 페이지 사이즈보다 작은경우
         *      마지막 페이지 일 경우(offext + 컨텐츠 사이즈를 더해서 전체 사이즈 구한다.)
         * */

        return new PageImpl<>(result.getResults(),pageable , countQuery.fetchCount());
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
