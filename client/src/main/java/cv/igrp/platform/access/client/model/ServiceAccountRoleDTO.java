package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One role held by a service account, as an id/code/department triple.
 *
 * <p>{@code departmentCode} is part of the identity, not decoration: role codes
 * are unique only within a department, so two distinct roles can share a code.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceAccountRoleDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("code")
    private String code;

    /** Department the role belongs to; null for a role with no department. */
    @JsonProperty("departmentCode")
    private String departmentCode;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
}
