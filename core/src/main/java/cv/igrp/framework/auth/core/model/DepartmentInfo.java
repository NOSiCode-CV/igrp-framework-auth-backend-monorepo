package cv.igrp.framework.auth.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentInfo {
    private String code;
    private String name;
    private String description;
    private String parentDepartment;
    private String status;
}
