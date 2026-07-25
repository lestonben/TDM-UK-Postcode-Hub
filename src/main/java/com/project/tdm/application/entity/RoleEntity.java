package com.project.tdm.application.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true)
    private String name;

    @Column(name = "role_desc", nullable = false, unique = true)
    private String description;

    @ManyToMany(mappedBy = "userRoles")
    private Set<UserEntity> users = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_pages",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "page_id")
    )
    private Set<PageEntity> allowedPages = new HashSet<>();

    public RoleEntity() {}

    public RoleEntity(String name, String description) {
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<UserEntity> getUsers() {
        return users;
    }

    public void setUsers(Set<UserEntity> users) {
        this.users = users;
    }

    public Set<PageEntity> getAllowedPages() {
        return allowedPages;
    }

    public void setAllowedPages(Set<PageEntity> allowedPages) {
        this.allowedPages = allowedPages;
    }

    public void addRole(PageEntity role) {
        this.allowedPages.add(role);
        role.getRoles().add(this);
    }

    public void removeRole(PageEntity role) {
        this.allowedPages.remove(role);
        role.getRoles().remove(this);
    }
}