package com.project.tdm.application.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pages")
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String urlPattern;

    @ManyToMany(mappedBy = "allowedPages")
    private Set<RoleEntity> roles = new HashSet<>();

    public PageEntity(){}

    public PageEntity(String name, String urlPattern) {
        this.name = name;
        this.urlPattern = urlPattern;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public Set<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleEntity> roles) {
        this.roles = roles;
    }

    public void addRole(RoleEntity role) {
        this.roles.add(role);
        role.getAllowedPages().add(this);
    }

    public void removeRole(RoleEntity role) {
        this.roles.remove(role);
        role.getAllowedPages().remove(this);
    }
}
