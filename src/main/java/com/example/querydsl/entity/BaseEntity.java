package com.example.querydsl.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;


@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
@Setter
public class BaseEntity extends BaseTimeEntity{ //시간은 거의 항상 필요하므로 따로 때놓는것을 추천!

    @CreatedBy
    @Column(updatable = false)
    private String createdBy; //등록자

    @LastModifiedBy
    private String LastModifiedBy;//수정자
}
