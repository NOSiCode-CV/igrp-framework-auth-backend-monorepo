package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.scope.IgrpScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

/** v3-14: an entity under the starter's scope filter, with the conventional column names. */
@Entity
@Table(name = "scoped_invoice")
@Filter(name = IgrpScope.FILTER)
public class ScopedInvoice {

    @Id
    private Long id;

    @Column(name = "owner_id")
    private String ownerId;

    @Column(name = "role_code")
    private String roleCode;

    @Column(name = "department_code")
    private String departmentCode;

    protected ScopedInvoice() {
    }

    public ScopedInvoice(Long id, String ownerId, String roleCode, String departmentCode) {
        this.id = id;
        this.ownerId = ownerId;
        this.roleCode = roleCode;
        this.departmentCode = departmentCode;
    }

    public Long getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }
}
