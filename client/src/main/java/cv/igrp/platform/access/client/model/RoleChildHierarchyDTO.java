package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleChildHierarchyDTO {

    @JsonProperty("roleCode")
    private String roleCode;

    @JsonProperty("departmentCode")
    private String departmentCode;

    @JsonProperty("children")
    private List<RoleChildHierarchyDTO> children;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public List<RoleChildHierarchyDTO> getChildren() {
        return children;
    }

    public void setChildren(List<RoleChildHierarchyDTO> children) {
        this.children = children;
    }
}
