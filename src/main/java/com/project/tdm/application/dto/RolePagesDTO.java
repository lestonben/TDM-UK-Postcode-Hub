package com.project.tdm.application.dto;

import java.util.Set;

public class RolePagesDTO {

    private Long id;
    private String roleName;
    private String roleDesc;
    private Set<String> pagesCanAccess;

    public RolePagesDTO(){}

    public RolePagesDTO(Long id, String roleName, String roleDesc, Set<String> pagesCanAccess) {
        this.id = id;
        this.roleName = roleName;
        this.roleDesc = roleDesc;
        this.pagesCanAccess = pagesCanAccess;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleDesc() {
        return roleDesc;
    }

    public void setRoleDesc(String roleDesc) {
        this.roleDesc = roleDesc;
    }

    public Set<String> getPagesCanAccess() {
        return pagesCanAccess;
    }

    public void setPagesCanAccess(Set<String> pagesCanAccess) {
        this.pagesCanAccess = pagesCanAccess;
    }
}
