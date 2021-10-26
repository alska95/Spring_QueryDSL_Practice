package com.example.querydsl;

import com.example.querydsl.dto.MemberDto;
import com.example.querydsl.dto.QMemberDto;
import com.example.querydsl.dto.UserDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.QTeam;
import com.example.querydsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.xmlunit.xpath.JAXPXPathEngine;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;


    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {

        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJPQL() {
        //member1 찾기
        Member findMember = em.createQuery(
                "select m from Member m " +
                        "where m.name =:name", Member.class)
                .setParameter("name", "member1")
                .getSingleResult();

        assertThat(findMember.getName()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl() {
        queryFactory = new JPAQueryFactory(em); // 동시성 관리되게 처리되어 있다.
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
    public void QTypes() {
        queryFactory = new JPAQueryFactory(em);
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
    public void search() {
        queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.name.eq("member1")
                                .and(member.age.eq(10))
                                .and(member.name.ne("abc"))
                                .and(member.name.eq("abc").not())
                                .and(member.age.in(10, 20)) // 20 > >= 10
                                .and(member.age.gt(1)) // > 1
                                .and(member.age.goe(1))
                                .and(member.age.loe(100))
                                .and(member.age.lt(100))
                                .and(member.name.like("member%")) //like검색
                                .and(member.name.contains("member")) // %member%
                        , member.name.startsWith("member") //,로 구분 지을경우 NULL을 무시해 주기 때문에 동적 쿼리를 매우 손쉽게 작성 가능하다.
                )
                .fetchOne();
        assertThat(findMember.getName()).isEqualTo("member1");

    }

    @Test
    public void getResult() {
        queryFactory = new JPAQueryFactory(em);
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
     * 단 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        queryFactory = new JPAQueryFactory(em);
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 90));
        em.persist(new Member("member6", 90));

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
    public void pageing() {
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
    public void aggregation() {
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
     */
    @Test
    public void group() {
        queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team(), team)
                .groupBy(team.name)
//                .having(member.age.gt(0))
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
    }

    /**
     * 조인의 기본문법
     * 조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고,
     * 두 번째 파라미터에 별칭으로 사용할 Q타입을 지정하면된다.
     */
    @Test
    public void join() {
        queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory.selectFrom(member)
                .join(member.team(), team)
//                .leftJoin(member.team(), team)
//                .rightJoin(member.team(), team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("name")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원을 조회 //억지 연관 없는애들 조인
     */
    @Test // cross join 카르테시안 곱
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.name.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("name")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인 하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory.select(member, team)
                .from(member)
                .leftJoin(member.team(), team) //join으로 바꾸면 innerjoin이 걸려서 teamA가 아닌애들은 아얘 안나옴.
                .on(team.name.eq("teamA")) //leftjoin시 반드시 on사용 where로 걸러버리면 다 사라지기 때문
                .fetch(); //결과가 튜플로 나오는 이유는 select가 여러가지 타입을 가지고 있기 때문.

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * 연관 관계가 없는 엔티티 외부조인
     */

    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory.select(member, team)
                .from(member)
                .leftJoin(team) //막조인 할꺼니까 member.team이렇게 안써도댐 on절은 join대상을 줄여준다. (member.team이렇게 쓰면 아이디(pk)를 매칭시켜주는것)
                .on(member.name.eq(team.name)).fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        /**
         * 일반 조인 : leftJoin(member.team, team).where(xxx)
         * on조인 : from(member).leftJoin(team).on(xxx)*/

        /**
         * Fetch Join
         * 패치 조인은 sql에서 제공하는 기능은 아니고, sql을 활용해서 연관된 엔티티를 sql 한번에 전부 조회하는 기능이다.
         * 주로 성능 최적화에 사용된다.
         * */
    }

    @Test
    public void noFetchJoin() {
        em.flush();
        em.clear();

        Member member1 = queryFactory.selectFrom(member)
                .where(member.name.eq("member1"))
                .fetchOne();

    }

    @Test
    public void FetchJoin() {
        em.flush();
        em.clear();

        Member member1 = queryFactory.selectFrom(member)
                .join(member.team(), team).fetchJoin()
                .where(member.name.eq("member1"))
                .fetchOne();

        System.out.println("member1 = " + member1);

    }

    /**
     * subQuery는 querydsl.jpa.JPAExpression을 통해 사용할 수 있다.
     * <p>
     * 1. 나이가 가장 많은 회원 조회
     * 2. 나이가 평균 이상인 회원 조회
     * 3. in 사용
     * 4. select절에 사용
     */
    @Test
    public void subQuery() {

        QMember memberSub = new QMember("membersub"); //allias 분리를 위함.
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions.select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        System.out.println("1 ================== result.get(0).getAge() = " + result.get(0).getAge());

        //goe
        List<Member> result2 = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions.select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        for (Member member1 : result2) {
            System.out.println("2 ================== member1.getAge() = " + member1.getAge());
        }

        //in절 사용
        List<Member> result3 = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(member.age.gt(10))
                ))
                .fetch();
        for (Member member1 : result3) {
            System.out.println("3 ================== member1.getAge() = " + member1.getAge());
        }
        //select절에 사용
        List<Tuple> result4 = queryFactory
                .select(member.name,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result4) {
            System.out.println("4 ================== tuple = " + tuple);
        }

        /**
         * JPA를 사용시, SUBquery한계:
         * from 절에서의 SubQuery(인라인 뷰)가 불가능하다.
         * 하이버네이트 구현체를 사용하면 select절의 서브쿼리는 지원한다.
         *
         * from 절의 서브쿼리 해결방법
         * 1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
         * 2. 애플리케이션 쿼리를 2번 분리해서 실행한다.
         * 3. nativeSQL을 사용한다.
         * */

    }

    /**
     * Case문
     */
    @Test
    public void caseQuery() {

        //단순한 조건
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("케이스문1 = " + s);
        }

        //복잡한 조건
        List<String> result2 = queryFactory.select(
                new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result2) {
            System.out.println("케이스문2 = " + s);
        }
    }

    /**
     * 상수 문자열 concat
     */

    @Test
    public void constant() {
        List<Tuple> a = queryFactory
                .select(member.name, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : a) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() {
        //원하는 형식 name_age
        List<String> fetch = queryFactory
                .select(member.name.concat("_").concat(member.age.stringValue()))    //concat은 문자열끼리 더해야 하므로 형변환을 해줘야한다. cast(member.age as char)
                .from(member)
                .where(member.name.eq("member1"))
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }


    /**
     * 프로잭션
     * 프로잭션으로 select 대상 type 지정 가능하다.
     * 프로젝션 대상이 하나일 경우, 타입을 명확하게 지정할 수 있다.
     * 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회한다.
     */
    @Test
    public void projection() {
        List<String> solType = queryFactory.select(member.name)
                .from(member)
                .fetch();//명확히 지정되어 나온다.

        List<Tuple> manyType = queryFactory.select(member.name, member.age)
                .from(member)
                .fetch();//튜플로 나온다

        for (Tuple tuple : manyType) {
            System.out.println("tuple = " + tuple);
            System.out.println("tuple.get(member.name) = " + tuple.get(member.name));
            System.out.println("tuple.get(member.age) = " + tuple.get(member.age));
        }
        /**
         * 튜플이 뭐냐하면, 타입을 여러개 지정해 놓을 경우를 대비하여 만들어 놓은 타입이다.
         * 튜플은 com.queryDsl.core꺼임 -> repository계층 안에서 쓰는건 괜찮은데,
         * 위로 넘어가면 안좋은 설계가 될 수 있다. 나중에 하부 기술을 교체할때, 전부 바꿔야되게 될 수도 있기때문에.
         * */
    }

    /**
     * 프로젝션과 결과 반환 -DTO조회
     */
    @Test
    public void projectionByDtoConstructorFields() {
//        em.createQuery("select new com.example.querydsl.dto.MemberDto(m.name , m.age) from Member m" , MemberDto.class); // JPA방식
        /*new명령어를 사용해야함, DTO의 package 이름을 다 적어줘야함, 생성자 방식만 지원함.
         * */

        List<MemberDto> result = queryFactory
//                .select(Projections.constructor(MemberDto.class 생성자 방식은 있는 생성자를 활용한다. 생성자를보고 들어가기 때문에 select 타입만 맞으면 된다.
//                .select(Projections.fields(MemberDto.class 필드는 기본 생성자가 필요가 없다. 필드로 넣기 때문에, 필드 명이 같아야 한다.
                .select(Projections.bean(MemberDto.class
                        , member.name
                        , member.age
                )) //Projections setter(bean에 데이터를 인잭션)로 dto값을 넣어주고, 값을 입력한다.
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDto() {
//        queryFactory.select(Projections.fields(UserDto.class 이렇게 조회하면 필드명이 다르기 때문에 에러가 난다.
        QMember subMember = new QMember("subMember");
        List<UserDto> userName = queryFactory.select(Projections.fields(UserDto.class,
                member.name.as("userName"),
                ExpressionUtils.as(JPAExpressions.select(subMember.age.max()).from(subMember), "age")
//                ,member.age
        ))
                .from(member)
                .fetch();

        for (UserDto userDto : userName) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * QueryProjection --> dto도 q타입으로 생성해서 사용하는법.
     * 장점 : 컴파일 시점에 오류 확인 할 수 있어서 안정적으로 코딩이 가능하다.
     * 단점 : dto에 QueryDsl에 대한 의존성이 생긴다.
     * dto같은 경우에는 여기저기 레이어에 걸쳐서 돌아다니는데, 거추장스럽다.
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> fetch = queryFactory.select(new QMemberDto(member.name, member.age))
                .from(member)
                .fetch(); //컴파일 시점에 오류 확인 가능하다.
    }

    /**
     * 동적 쿼리 처리하기
     * 처리하는 두가지 방식
     * 1. BooleanBuilder
     * 2. where 다중 파라미터 사용*/
    @Test
    public void 동적쿼리_BooleanBuilder(){
        String nameParam = "member1";
        Integer ageParam = 10;

        List<Member> members = searchMember1(nameParam, ageParam);
        for (Member member1 : members) {
            System.out.println("member1 = " + member1);
        }

    }

    public List<Member> searchMember1(String nameCond, Integer ageCond){
        BooleanBuilder builder = new BooleanBuilder(); // 초기값을 넣을 수도 있다.
        if(nameCond != null){
            builder.and(member.name.eq(nameCond));
        }
        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory.selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * where 다중 파라미터
     * 장점 :   사용하면 코드가 아주 깔끔해진다!
     *         재사용성이 높다.
     * where안에서 조건을 풀어버린다.
     * null이면 null로, 조건이면 조건으로
     * where 안에 null 이 들어가면 ex) where(null , null ,...)
     * null은 무시가된다. --> 동적쿼리 가능
   * */
    @Test
    public void 동적쿼리_where다중파라미터(){
        String nameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(nameParam , ageParam);

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    private List<Member> searchMember2(String nameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(nameEq(nameCond), ageEq(ageCond))
//                .where(allEq(nameCond, ageCond))
                .fetch();
    }

    private BooleanExpression nameEq(String nameCond){
        return nameCond != null ? member.name.eq(nameCond) : null;
    }


    private BooleanExpression ageEq(Integer ageCond){
        return ageCond != null ? member.age.eq(ageCond) : null;
    }


    private BooleanExpression allEq(String nameCond, Integer ageCond){
        return nameEq(nameCond)
                .and(ageEq(ageCond));
    }

    /**
     * 원래 업데이트는 Dirty 체크 사용해서 주로 하였다.
     * DirtyCheck 방식은 엔티티 하나당 각각 나가므로 동시에 여러개 수정할때는 효율적이지 않다.
     * 뒤에 execute붙여서 실행 가능.
     * 영향을 받은 row수를 반환한다.
     * 주의 해야 하는점 :  bulk연산은 영속성 컨텍스트를 사용하지 않는다.
     *                  bulk연산 후에, 영속성 컨텍스트의 상태와 db의 상태를 필요에 따라 동기화 시켜줘야 한다.
     *                  그렇다고 조회시에 다른 결과가 조회되는 것은 아니다.
     *                  왜냐하면 영속성 컨텍스트가 우선순위를 가져서 영속성컨텍스트 안에 있는 엔티티를
     *                  db에서 조회할 경우 db조회 결과를 버리고 영속성 컨텍스트 안의 엔티티를 반환한다.
     *                          --> Repeatable Read 격리수준(다른 트랜잭션에서 update날려서 변경해도 영속성 컨텍스트를
     *                                                      조회하기 때문에 만족)
    * */
    @Test
    public void bulkFunction(){
        long count = queryFactory
                .update(member)
                .set(member.name, "황경하")
                .where(member.age.gt(1))
                .execute();

        em.flush(); //db와 동기화
        em.clear(); //영속성 컨텍스트 초기화

        long count2 = queryFactory.delete(member)
                .where(member.age.gt(100))
                .execute();
    }

    @Test
    public void bulkAddition(){
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    /**
     * sql function 호출하기
     *
     * Dialect에 등록된방언만 사용할 수 있다.
     * 1.member의 m을 M으로 변경해달라.
     *
     * */
    @Test
    public void sqlFunctionReplace(){
        String result = queryFactory
                .select(Expressions
                        .stringTemplate("function('replace',{0},{1},{2})", member.name, "member", "M"))
                .from(member)
                .fetchFirst();
        System.out.println("result = " + result);
    }

    @Test
    public void sqlFunctionLower(){
        List<String> fetch = queryFactory
                .select(member.name)
                .from(member)
                .where(member.name.eq(Expressions.stringTemplate("function('lower',{0})", member.name)))
//                .where(member.name.eq(member.name.lower())) 안씨 표준문법에 있는 것 들은 대부분 default로 내장함.
                .fetch();
    }
}