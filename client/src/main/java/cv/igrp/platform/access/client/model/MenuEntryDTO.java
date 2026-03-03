package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import cv.igrp.platform.access.client.constants.MenuEntryType;
import cv.igrp.platform.access.client.constants.Status;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MenuEntryDTO {
    @JsonProperty("id")
    private Integer id;
    
    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private MenuEntryType type;

    @JsonProperty("position")
    private Integer position;

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("target")
    private String target;

    @JsonProperty("url")
    private String url;

    @JsonProperty("pageSlug")
    private String pageSlug;

    @JsonProperty("parentCode")
    private String parentCode;

    @JsonProperty("applicationCode")
    private String applicationCode;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;

    @JsonProperty("lastModifiedDate")
    private String lastModifiedDate;

    @JsonProperty("roles")
    private List<RoleDepartmentDTO> roles;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MenuEntryType getType() {
        return type;
    }

    public void setType(MenuEntryType type) {
        this.type = type;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;}

    public String getPageSlug() {
        return pageSlug;
    }

    public void setPageSlug(String pageSlug) {
        this.pageSlug = pageSlug;}

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;}

    public String getCreatedBy() {
        return createdBy;}

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;}

    public String getCreatedDate() {
        return createdDate;}

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;}

    public String getLastModifiedBy() {
        return lastModifiedBy;}

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;}

    public String getLastModifiedDate() {
        return lastModifiedDate;}

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;}

    public List<RoleDepartmentDTO> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleDepartmentDTO> roles) {
        this.roles = roles;
    }


}