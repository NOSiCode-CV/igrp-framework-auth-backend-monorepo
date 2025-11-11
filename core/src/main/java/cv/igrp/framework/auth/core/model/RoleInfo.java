package cv.igrp.framework.auth.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleInfo {
    private String name;
    private String description;
    private String departmentCode;
    private String status;
}