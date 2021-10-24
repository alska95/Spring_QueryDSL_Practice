package com.example.querydsl.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


@Getter
@Setter
@Entity
public class Hello {
    //쿼리 dsl은 큐타입을 뽑아내고 그걸가지고 쿼리를 날린다. 그것을 확인.
    //QHello로 빌드시에 생성된다.

    @Id @GeneratedValue
    private Long id;


}
