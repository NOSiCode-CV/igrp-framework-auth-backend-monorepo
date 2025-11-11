package cv.igrp.framework.auth.wso2;

import cv.igrp.framework.auth.core.adapter.IAdapter;
import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.UserIdentity;
import cv.igrp.framework.auth.wso2.application.Application;
import cv.igrp.framework.auth.wso2.application.ApplicationService;
import cv.igrp.framework.auth.wso2.application.dto.CreateApplicationRequest;
import cv.igrp.framework.auth.wso2.application.dto.UpdateApplicationRequest;
import cv.igrp.framework.auth.wso2.group.GroupService;
import cv.igrp.framework.auth.wso2.role.Role;
import cv.igrp.framework.auth.wso2.role.RoleService;
import cv.igrp.framework.auth.wso2.shared.Resource;
import cv.igrp.framework.auth.wso2.shared.Wso2ApiResponse;
import cv.igrp.framework.auth.wso2.user.UserMapper;
import cv.igrp.framework.auth.wso2.user.UserService;
import cv.igrp.framework.auth.wso2.user.dto.Wso2UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public abstract class Wso2Adapter implements IAdapter {

    private final ApplicationService applicationService;
    private final UserService userService;
    private final RoleService roleService;
    private final GroupService groupService;
    private static final Logger log = LoggerFactory.getLogger(Wso2Adapter.class);

    public Wso2Adapter(ApplicationService applicationService, UserService userService, RoleService roleService, GroupService groupService) {
        this.applicationService = applicationService;
        this.userService = userService;
        this.roleService = roleService;
        this.groupService = groupService;
    }

    @Override
    public void createApplication(String name) throws IAMException {
        log.info("Create Application with name: [{}]", name);
        CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest(name);
        Wso2ApiResponse<Application> result = applicationService.createApplication(createApplicationRequest);
        if (!result.isSuccessfulCall()) {
            throw new IAMException(result.getFailureMessage());
        }
        Wso2ApiResponse<Resource> applicationGroup = groupService.createGroup(createApplicationRequest.getName());
        if (!applicationGroup.isSuccessfulCall()) {
            log.error("Error creating Application Group with Application name: [{}]", name);
            throw new IAMException("Application created but failed to create group: " + applicationGroup.getFailureMessage());
        }
        log.info("Created Application: [{}], id: [{}]. Default Group id: [{}] created successfully",
                result.getResourceResponse().name(), result.getResourceResponse().id(), applicationGroup.getResourceResponse().id());
    }

    @Override
    public void deleteApplication(String name) throws IAMException {
        log.info("Delete Application with name: [{}].", name);
        Optional<Application> optionalApplication = applicationService.getApplicationByName(new Application(null, name));
        if (optionalApplication.isEmpty()) {
            log.warn("Delete Application - Application with name: [{}] not found.", name);
            throw new IAMException("Delete Application - Application with name: " + name + " not found.");
        }
        Wso2ApiResponse<Application> result = applicationService.deleteApplication(optionalApplication.get());
        if (!result.isSuccessfulCall()) {
            throw new IAMException(result.getFailureMessage());
        }
        Optional<Resource> optionalResource = groupService.getGroupByName(name);
        if (optionalResource.isEmpty()) {
            log.error("Delete Application - Application Group with name: [{}] not found.", name);
            throw new IAMException("Delete Application - Application Group with name: " + name + " not found.");
        }
        Wso2ApiResponse<Resource> deleteApplicationGroupResponse = groupService.deleteGroup(optionalResource.get());
        if (!deleteApplicationGroupResponse.isSuccessfulCall()) {
            log.error("Delete Application - Application Group with name: [{}] not found.", name);
            throw new IAMException("Delete Application - Application Group with name: " + name + " not found.");
        }
        log.info("Application with name: [{}] deleted successfully.", name);
    }

    @Override
    public void updateApplication(String name, String newName) throws IAMException {
        log.info("Update Application with name: [{}].", name);
        Optional<Application> optionalApplication = applicationService.getApplicationByName(new Application(null, name));
        if (optionalApplication.isEmpty()) {
            log.warn("Update Application - Application with name: [{}] not found.", name);
            throw new IAMException("Update Application - Application with name: " + name + " not found.");
        }
        Wso2ApiResponse<Application> result = applicationService.updateApplication(optionalApplication.get(), new UpdateApplicationRequest(newName));
        if (!result.isSuccessfulCall()) {
            throw new IAMException(result.getFailureMessage());
        }
        Optional<Resource> optionalApplicationGroup = groupService.getGroupByName(optionalApplication.get().name());
        if (optionalApplicationGroup.isEmpty()) {
            log.warn("Update Application - Application Group: [{}] not found.", name);
            throw new IAMException("Update Application - Application Group: " + name + " not found.");
        }
        Wso2ApiResponse<Resource> applicationGroupResponse = groupService.updateGroupName(optionalApplicationGroup.get(),
                new Resource(optionalApplication.get().id(), newName));
        if (!applicationGroupResponse.isSuccessfulCall()) {
            log.error("Update Application Group name: [{}] id: [{}] .", optionalApplicationGroup.get().name(), optionalApplicationGroup.get().id());
            throw new IAMException("Update Application - Application with name: " + name + " not found.");
        }
        log.info("Application with name: [{}] updated successfully.", optionalApplication.get().name());
    }

    @Override
    public void createDepartment(String applicationName, String departmentName) throws IAMException {
        log.info("Create Department with Application name: [{}]. Department name: [{}]", applicationName, departmentName);
        Optional<Application> optionalApplication = applicationService.getApplicationByName(new Application(null, applicationName));
        if (optionalApplication.isEmpty()) {
            log.warn("Create Department - Application with name: [{}] not found.", applicationName);
            throw new IAMException("Create Department - Application with name: " + applicationName + " not found.");
        }
        Application savedApplication = optionalApplication.get();
        Wso2ApiResponse<Resource> createGroupResponse = groupService.createGroup(departmentName);
        if (!createGroupResponse.isSuccessfulCall()) {
            log.error("Unable to create Department: [{}].", departmentName);
            throw new IAMException(createGroupResponse.getFailureMessage());
        }
        log.info("Department with name: [{}], Application name: [{}] created successfully.", departmentName, savedApplication.name());
    }

    @Override
    public void createDepartment(String applicationName, String departmentName, String parentDepartment) throws IAMException {
    }

    @Override
    public void deleteDepartment(String departmentName) throws IAMException {
        log.info("Delete Department with name: [{}]", departmentName);
        Optional<Resource> optionalResource = groupService.getGroupByName(departmentName);
        if (optionalResource.isEmpty()) {
            log.warn("Department with name: [{}] not found", departmentName);
            throw new IAMException("Department with name: " + departmentName + " not found.");
        }
        Wso2ApiResponse<Resource> resourceWso2ApiResponse = groupService.deleteGroup(optionalResource.get());
        if (!resourceWso2ApiResponse.isSuccessfulCall()) {
            log.error("Unable to delete Department with name: [{}].", departmentName);
            throw new IAMException("Unable to delete Department with name: " + departmentName);
        }
        log.error("Department with name [{}] deleted successfully.", departmentName);
    }

    @Override
    public void updateDepartment(String departmentName, String departmentNewName) throws IAMException {
        log.info("Update Department: [{}] with new Name: [{}]", departmentName, departmentNewName);
        Optional<Resource> optionalResource = groupService.getGroupByName(departmentName);
        if (optionalResource.isEmpty()) {
            log.warn("Department with name: [{}] not found", departmentName);
            throw new IAMException("Department with name: " + departmentName + " not found.");
        }
        Wso2ApiResponse<Resource> updateDepartmentResponse = groupService.updateGroupName(optionalResource.get(),
                new Resource(optionalResource.get().id(), departmentNewName));
        if (!updateDepartmentResponse.isSuccessfulCall()) {
            log.error(updateDepartmentResponse.getFailureMessage());
            throw new IAMException(updateDepartmentResponse.getFailureMessage());
        }
        log.info("Department: [{}] updated with new Name: [{}] successfully.", departmentName, departmentNewName);
    }

    @Override
    public void createRole(String applicationName, String departmentName, String roleName) throws IAMException {
        log.info("Create Role with ApplicationName: [{}], Department name: [{}], Role name: [{}]", applicationName, departmentName, roleName);
        Optional<Application> optionalApplication = applicationService.getApplicationByName(new Application(null, applicationName));
        if (optionalApplication.isEmpty()) {
            log.error("Application: [{}] not found.", applicationName);
            throw new IAMException("Application: " + applicationName + " not found.");
        }
        Resource department = groupService.getGroupByName(departmentName)
                .orElseThrow(() -> {
                    log.error("Department: [{}] not found.", departmentName);
                    return new IAMException("Department: " + departmentName + " not found.");
                });
        Role newRole = new Role(null, roleName);
        Wso2ApiResponse<Resource> createRoleResponse = roleService.createRole(newRole);
        if (!createRoleResponse.isSuccessfulCall()) {
            log.error(createRoleResponse.getFailureMessage());
            throw new IAMException(createRoleResponse.getFailureMessage());
        }
        log.info("Role name: [{}] id: [{}] created successfully.", roleName, createRoleResponse.getResourceResponse().id());
    }

    @Override
    public void deleteRole(String roleName) throws IAMException {
        log.info("Delete Role: [{}]", roleName);
        Role foundRole = roleService.findByName(roleName)
                .orElseThrow(() -> {
                    log.warn("Role : [{}] not found.", roleName);
                    return new IAMException("Role:  " + roleName + " not found.");
                });
        Wso2ApiResponse<Resource> deleteRoleResponse = roleService.deleteRole(foundRole);
        if (!deleteRoleResponse.isSuccessfulCall()) {
            log.warn(deleteRoleResponse.getFailureMessage());
            throw new IAMException(deleteRoleResponse.getFailureMessage());
        }
        log.info("Deleted Role: [{}] id: [{}]", foundRole.name(), foundRole.id());
    }

    @Override
    public void assignRoleToUser(String roleName, String userId) throws IAMException {
        log.info("Assign Role to User with role name: [{}], user id: [{}]", roleName, userId);
        Optional<Role> optionalRole = roleService.findByName(roleName);
        if (optionalRole.isEmpty()) {
            log.warn("Role with name [{}] not found.", roleName);
            throw new IAMException("Role with name: " + roleName + " not found.");
        }
        Wso2ApiResponse<Wso2UserResponse> findUserByIdResponse = userService.findUserById(userId);
        if (!findUserByIdResponse.isSuccessfulCall()) {
            log.error("user with id: [{}] failure: [{}]", userId, findUserByIdResponse.getFailureMessage());
            throw new IAMException(findUserByIdResponse.getFailureMessage());
        }
        Wso2ApiResponse<Resource> assignRoleToUserResponse = roleService.assignRoleToUser(optionalRole.get(), findUserByIdResponse.getResourceResponse());
        if (!assignRoleToUserResponse.isSuccessfulCall()) {
            log.error(findUserByIdResponse.getFailureMessage());
            throw new IAMException(assignRoleToUserResponse.getFailureMessage());
        }
        log.info("Role id: [{}] assigned to User id: [{}] successfully.", optionalRole.get().id(), userId);
    }

    @Override
    public Optional<UserIdentity> resolveUser(String uid) {
        log.info("Resolve User with id: [{}]", uid);
        Wso2ApiResponse<Wso2UserResponse> response = userService.findUserById(uid);
        if (!response.isSuccessfulCall()) {
            log.error("Resolve user with id: [{}] failure: [{}]", uid, response.getFailureMessage());
            return Optional.empty();
        }
        if (response.isSuccessfulCall() && response.getResourceResponse() != null) {
            Wso2UserResponse resourceResponse = response.getResourceResponse();
            log.info("Found User with id: [{}] and name: [{}]", uid, resourceResponse.getName().getGivenName());
            return Optional.of(UserMapper.userToUserIdentity(resourceResponse));
        }
        log.info("User with id: [{}] not found.", uid);
        return Optional.empty();
    }
}
