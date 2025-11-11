package cv.igrp.framework.auth.wso2;

import cv.igrp.framework.auth.core.adapter.IAdapter;
import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.UserIdentity;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

/*@SpringBootApplication*/
public class Main {

    private final Wso2Adapter wso2Adapter;

    public Main(Wso2Adapter wso2Adapter) {
        this.wso2Adapter = wso2Adapter;
    }

    public static void main(String[] args) throws IAMException {
        ApplicationContext context = SpringApplication.run(Main.class, args);
        IAdapter adapter = context.getBean(IAdapter.class);

        String applicationName = "atr";
        String applicationNewName = "app_loloda_3";
        String financeDepartment = "finance";
        String marketingDepartment = "marketingevolve";
        String createRoleName = "create";

        /*
        adapter.createDepartment("app_" + applicationName, marketingDepartment);
        adapter.createApplication(applicationName);
        adapter.deleteApplication(applicationNewName);
        adapter.updateApplication(applicationName, applicationNewName);
        adapter.deleteDepartment(financeDepartment);
        adapter.updateDepartment("app_"+ applicationName + "_" + marketingDepartment, marketingDepartmentNewName);
        */
        /*adapter.createRole("app_" + applicationName, "app_" + applicationName + "_" + marketingDepartment, createRoleName);
        adapter.assignRoleToUser("app_atr_marketingevolve_create", "88c6ecd7-8fec-4975-9cdc-6b4f066d52c9");
         */
        Optional<UserIdentity> userIdentity = adapter.resolveUser("88c6ecd7-8fec-4975-9cdc-6b4f066d52c9");
        System.out.println(userIdentity.get().getUsername());
    }
}
