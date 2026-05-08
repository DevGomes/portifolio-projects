package com.brenogomes.pm.model.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "member")
@Data
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 200)
    private String email;

    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    private List<Project> projects = new ArrayList<>();
}
