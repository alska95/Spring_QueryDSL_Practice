package com.example.querydsl.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@AllArgsConstructor
@Data
@Entity
public class Team {
    @Id @GeneratedValue
    @Column(name ="team_id")
    private Long id;

    private String name;
    public Team(String name){
        this.name = name;
    }
    public Team() {
    }
}
