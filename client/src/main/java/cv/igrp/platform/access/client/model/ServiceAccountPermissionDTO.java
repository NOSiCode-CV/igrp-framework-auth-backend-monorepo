package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One permission granted directly to a service account, as an id/name pair.
 *
 * <p>No department is carried: direct grants are not department-scoped, a service
 * account may hold any permission in the system.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceAccountPermissionDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
