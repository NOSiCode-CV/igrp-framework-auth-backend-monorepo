package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleParentHierarchyDTO {

    @JsonProperty("roleCode")
    private String roleCode;

    @JsonProperty("departmentCode")
    private String departmentCode;

    @JsonProperty("parents")
    private List<RoleParentHierarchyDTO> parents;

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

    public List<RoleParentHierarchyDTO> getParents() {
        return parents;
    }

    public void setParents(List<RoleParentHierarchyDTO> parents) {
        this.parents = parents;
    }
}