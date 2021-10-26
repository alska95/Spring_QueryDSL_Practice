package com.example.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {
    private String name;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
