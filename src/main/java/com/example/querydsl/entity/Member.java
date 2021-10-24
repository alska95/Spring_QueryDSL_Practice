package com.example.querydsl.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;


@Entity
@Table(name = "members")
@Getter
@Setter
@AllArgsConstructor
@NamedQuery(
        name = "Member.findByName",
        query = "select m from Member m where m.name =: name"
)
@NamedEntityGraph(name = "member.all", attributeNodes = @NamedAttributeNode("team"))
public class Member extends BaseEntity{
    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String name;
    private int age;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String name, int age, Team team){
        this.name = name;
        this.age = age;
        this.team = team;
    }
    public Member(String name){
        this.name = name;
    }


    public Member(String name, int age){
        this.name = name;
        this.age = age;
    }

    public Member(){

    } //스팩상 protected로 -->JPA구현체들이 객체를 만들어낼때 사용하기 위함.

}
