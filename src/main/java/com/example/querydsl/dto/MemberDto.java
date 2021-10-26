package com.example.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor //getConstructor0에러 NoSuchMethodException
// 쿼리 dsl이 빈memberDto를 만든다음에 set으로 값을 집어넣어야 하는데, 기본 생성자가 없어서 애러 발생
public class MemberDto {
    private String name;
    private int age;

    @QueryProjection //dto도 Q파일로 생성이 가능하다.
    public MemberDto(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
