package cv.igrp.framework.auth.core.adapter;

import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;

/**
 * IAdapter Interface for Interacting with Identity and Access Management (IAM) Systems.
 * This interface provides a common set of methods to interact with different IAM systems.
 * The methods define operations for managing departments, applications, roles, users, and permissions.
 */
@SuppressWarnings("unused")
public interface IAdapter {

  /**
   * Creates a department with the given code.
   *
   * @param departmentCode the code of the department to create
   * @param parentDepartment the code of the parent department (can be null for root departments)
   * @throws IAMException if there is an issue with the IAM system while creating the department
   */
  void createDepartment(String departmentCode, String parentDepartment) throws IAMException;

  /**
   * Updates a department's code.
   *
   * @param departmentCode the current code of the department
   * @param newCode the new code for the department
   * @throws IAMException if there is an issue with the IAM system while updating the department
   */
  void updateDepartment(String departmentCode, String newCode) throws IAMException;

  /**
   * Deletes a department.
   *
   * @param departmentCode the code of the department to delete
   * @throws IAMException if there is an issue with the IAM system while deleting the department
   */
  void deleteDepartment(String departmentCode) throws IAMException;

  /**
   * Creates an application within a department.
   *
   * @param departmentCode the code of the department where the application will be created
   * @param applicationCode the code of the application to create
   * @throws IAMException if there is an issue with the IAM system while creating the application
   */
  void createApplication(String departmentCode, String applicationCode) throws IAMException;

  /**
   * Updates an application's code.
   *
   * @param departmentCode the code of the department containing the application
   * @param applicationCode the current code of the application
   * @param newCode the new code for the application
   * @throws IAMException if there is an issue with the IAM system while updating the application
   */
  void updateApplication(String departmentCode, String applicationCode, String newCode) throws IAMException;

  /**
   * Deletes an application.
   *
   * @param departmentCode the code of the department containing the application
   * @param applicationCode the code of the application to delete
   * @throws IAMException if there is an issue with the IAM system while deleting the application
   */
  void deleteApplication(String departmentCode, String applicationCode) throws IAMException;

  /**
   * Creates a role within a department.
   *
   * @param departmentCode the code of the department where the role will be created
   * @param roleName the name of the role to create
   * @throws IAMException if there is an issue with the IAM system while creating the role
   */
  void createRole(String departmentCode, String roleName) throws IAMException;

  /**
   * Updates a role's name.
   *
   * @param departmentCode the code of the department containing the role
   * @param roleName the current name of the role
   * @param newName the new name for the role
   * @throws IAMException if there is an issue with the IAM system while updating the role
   */
  void updateRole(String departmentCode, String roleName, String newName) throws IAMException;

  /**
   * Deletes a role.
   *
   * @param departmentCode the code of the department containing the role
   * @param roleName the name of the role to delete
   * @throws IAMException if there is an issue with the IAM system while deleting the role
   */
  void deleteRole(String departmentCode, String roleName) throws IAMException;

  /**
   * Assigns a role to a user.
   *
   * @param departmentCode the code of the department containing the role
   * @param roleName the name of the role to assign
   * @param sub the user subject (sub)
   * @throws IAMException if there is an issue with the IAM system while assigning the role
   */
  void assignRoleToUser(String departmentCode, String roleName, String sub) throws IAMException;

  /**
   * Unassigns a role from a user.
   *
   * @param departmentCode the code of the department containing the role
   * @param roleName the name of the role to unassign
   * @param sub the user subject (sub or email)
   * @throws IAMException if there is an issue with the IAM system while unassigning the role
   */
  void unassignRoleFromUser(String departmentCode, String roleName, String sub) throws IAMException;

  /**
   * Creates a user with provided data.
   *
   * @param userIdentity the data of the user to create
   * @throws IAMException if there is an issue with the IAM system while creating the user
   */
  void createUser(UserIdentity userIdentity) throws IAMException;

  /**
   * Resolves a user based on the user identifier.
   *
   * @param email the user email
   * @return an Optional containing the user identity if found
   */
  Optional<UserIdentity> resolveUser(String email);

  /**
   * Creates a permission with a given name and description.
   *
   * @param permissionName the name of the permission to create
   * @param description the description of the permission
   * @throws IAMException if there is an issue with the IAM system while creating the permission
   */
  void createPermission(String permissionName, String description) throws IAMException;

  /**
   * Deletes a permission.
   *
   * @param permissionName the name of the permission to delete
   * @throws IAMException if there is an issue with the IAM system while deleting the permission
   */
  void deletePermission(String permissionName) throws IAMException;

  /**
   * Assigns a permission to multiple roles.
   *
   * @param permissionName the name of the permission to assign
   * @param roleNames the set of role names to assign the permission to
   * @throws IAMException if there is an issue with the IAM system while assigning the permission
   */
  void assignPermissionToRoles(String permissionName, Set<String> roleNames) throws IAMException;

  /**
   * Assigns multiple permissions to a single role.
   *
   * @param permissionNames the set of permission names to assign
   * @param roleName the role name to assign the permissions to
   * @throws IAMException if there is an issue with the IAM system while assigning the permissions
   */
  void assignPermissionsToRole(Set<String> permissionNames, String roleName) throws IAMException;

  /**
   * Unassigns a permission from multiple roles.
   *
   * @param permissionName the name of the permission to unassign
   * @param roleNames the set of role names to unassign the permission from
   * @throws IAMException if there is an issue with the IAM system while unassigning the permission
   */
  void unassignPermissionFromRoles(String permissionName, Set<String> roleNames) throws IAMException;

  /**
   * Unassigns multiple permissions from a single role.
   *
   * @param permissionNames the set of permission names to unassign
   * @param roleName the role name to unassign the permissions from
   * @throws IAMException if there is an issue with the IAM system while unassigning the permissions
   */
  void unassignPermissionsFromRole(Set<String> permissionNames, String roleName) throws IAMException;

  /**
   * Creates a resource with associated URIs and scopes.
   *
   * @param resourceName the name of the resource to create
   * @param description the description of the resource
   * @param uris the list of URIs associated with the resource
   * @param scopes the list of scopes applicable to the resource
   * @throws IAMException if there is an issue with the IAM system while creating the resource
   */
  void createResource(String resourceName, String description, List<String> uris, List<String> scopes) throws IAMException;

  /**
   * Deletes a resource.
   *
   * @param resourceName the name of the resource to delete
   * @throws IAMException if there is an issue with the IAM system while deleting the resource
   */
  void deleteResource(String resourceName) throws IAMException;

  /**
   * Retrieves all departments with their information.
   *
   * @return list of department information
   * @throws IAMException if there are issue retrieving departments
   */
  List<DepartmentInfo> getAllDepartments() throws IAMException;

  /**
   * Retrieves a specific department by code.
   *
   * @param departmentCode the department code
   * @return DepartmentInfo containing department details
   * @throws IAMException if department not found or retrieval fails
   */
  Optional<DepartmentInfo> getDepartment(String departmentCode) throws IAMException;

  /**
   * Retrieves all applications with their information.
   *
   * @return list of application information
   * @throws IAMException if there are issue retrieving applications
   */
  List<ApplicationInfo> getAllApplications() throws IAMException;

  /**
   * Retrieves a specific application by code.
   *
   * @param departmentCode the department code
   * @param applicationCode the application code
   * @return ApplicationInfo containing application details
   * @throws IAMException if application not found or retrieval fails
   */
  Optional<ApplicationInfo> getApplication(String departmentCode, String applicationCode) throws IAMException;

  /**
   * Retrieves all roles with their information.
   *
   * @return list of role information
   * @throws IAMException if there are issue retrieving roles
   */
  List<RoleInfo> getAllRoles() throws IAMException;

  /**
   * Retrieves a specific role by name.
   *
   * @param departmentCode the department code
   * @param roleName the role name
   * @return RoleInfo containing role details
   * @throws IAMException if role not found or retrieval fails
   */
  Optional<RoleInfo> getRole(String departmentCode, String roleName) throws IAMException;

  /**
   * Retrieves all users.
   *
   * @return list of UserIdentity objects
   * @throws IAMException if there's an issue retrieving users
   */
  List<UserIdentity> getAllUsers() throws IAMException;

  /**
   * Retrieves roles assigned to a user.
   *
   * @param sub the user subject
   * @return map where keys are department codes and values are role names
   * @throws IAMException if there are issue retrieving user roles
   */
  Map<String, Set<String>> getUserRoles(String sub) throws IAMException;

  /**
   * Retrieves users assigned to a role.
   *
   * @param departmentCode the department code
   * @param roleName the role name
   * @return set of subs
   * @throws IAMException if there are issue retrieving role users
   */
  Set<String> getRoleUsers(String departmentCode, String roleName) throws IAMException;

  /**
   * Retrieves all user-role assignments.
   *
   * @return map where keys are subs and values are maps of department to role sets
   * @throws IAMException if there's an issue retrieving all user roles
   */
  Map<String, Map<String, Set<String>>> getAllUserRoles() throws IAMException;

  /**
   * Retrieves all permissions with their information.
   *
   * @return list of permission information
   * @throws IAMException if there's an issue retrieving permissions
   */
  List<PermissionInfo> getAllPermissions() throws IAMException;

  /**
   * Retrieves a specific permission by name.
   *
   * @param permissionName the permission name
   * @return PermissionInfo containing permission details
   * @throws IAMException if permission not found or retrieval fails
   */
  Optional<PermissionInfo> getPermission(String permissionName) throws IAMException;

  /**
   * Retrieves roles associated with a permission.
   *
   * @param permissionName the permission name
   * @return set of role names
   * @throws IAMException if there's an issue retrieving permission roles
   */
  Set<String> getPermissionRoles(String permissionName) throws IAMException;

  /**
   * Retrieves permissions associated with a role.
   *
   * @param departmentCode the department code
   * @param roleName the role name
   * @return set of permission names
   * @throws IAMException if there's an issue retrieving role permissions
   */
  Set<String> getRolePermissions(String departmentCode, String roleName) throws IAMException;

  /**
   * Retrieves all role-permission assignments.
   *
   * @return map where keys are role names and values are permission sets
   * @throws IAMException if there's an issue retrieving all role permissions
   */
  Map<String, Set<String>> getAllRolePermissions() throws IAMException;

  /**
   * Retrieves all resources with their information.
   *
   * @return list of resource information
   * @throws IAMException if there are issue retrieving resources
   */
  List<ResourceInfo> getAllResources() throws IAMException;

  /**
   * Retrieves details of a specific resource.
   *
   * @param resourceName the resource name
   * @return ResourceInfo containing URIs and scopes
   * @throws IAMException if there's an issue retrieving the resource
   */
  Optional<ResourceInfo> getResource(String resourceName) throws IAMException;

  /**
   * Resets the provider by deleting all managed entities.
   * This should be called before a full synchronization to ensure a clean state.
   *
   * @throws IAMException if reset fails
   */
  void resetProvider() throws IAMException;

  /**
   * Checks if a department exists.
   *
   * @param departmentCode the department code
   * @return true if the department exists
   * @throws IAMException if check fails
   */
  boolean departmentExists(String departmentCode) throws IAMException;

  /**
   * Checks if an application exists.
   *
   * @param departmentCode the department code
   * @param applicationCode the application code
   * @return true if the application exists
   * @throws IAMException if check fails
   */
  boolean applicationExists(String departmentCode, String applicationCode) throws IAMException;

  /**
   * Checks if a role exists.
   *
   * @param departmentCode the department code
   * @param roleName the role name
   * @return true if the role exists
   * @throws IAMException if check fails
   */
  boolean roleExists(String departmentCode, String roleName) throws IAMException;

  /**
   * Checks if a permission exists.
   *
   * @param permissionName the permission name
   * @return true if the permission exists
   * @throws IAMException if check fails
   */
  boolean permissionExists(String permissionName) throws IAMException;

  /**
   * Checks if a resource exists.
   *
   * @param resourceName the resource name
   * @return true if the resource exists
   * @throws IAMException if check fails
   */
  boolean resourceExists(String resourceName) throws IAMException;

  /**
   * Checks if a protocol mapper exists.
   *
   * @param name the protocol mapper name
   * @return true if the protocol mapper exists
   * @throws IAMException if the check fails
   */
  boolean protocolMapperExists(String name) throws IAMException;

  /**
   * Creates a mapper for JWT iGRP roles claims
   *
   * @param claimName the claim name in the JWT
   * @param claimDescription the mapper's description
   * @throws IAMException if the creation fails
   */
  void createJwtRolesClaimMapper(String claimName, String claimDescription) throws IAMException;

  /**
   * Get all the client's protocol mappers for JWT
   *
   * @return List<String> a list with the name of the mappers
   * @throws IAMException if the fetch fails
   */
  List<String> getAllClientProtocolMappers() throws IAMException;

}