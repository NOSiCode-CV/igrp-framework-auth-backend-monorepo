package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleDepartmentDTO {
    @JsonProperty("roleCode")
    private String roleCode;

    @JsonProperty("departmentCode")
    private String departmentCode;

    public String getRoleCode() { return roleCode; }

    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getDepartmentCode() { return departmentCode; }

    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

}
