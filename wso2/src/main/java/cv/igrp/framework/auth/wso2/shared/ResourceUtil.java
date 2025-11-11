package cv.igrp.framework.auth.wso2.shared;

public class ResourceUtil {

    public static final String RESOURCE_NAME_REGEX = "^[a-zA-Z0-9]+$";
    private static final String APPLICATION_PREFIX = "app_";

    public static String extractApplicationName(String departmentFullName) {
        if (departmentFullName == null || !departmentFullName.startsWith("app_")) {
            throw new IllegalArgumentException("Invalid department name format.");
        }

        int firstUnderscore = departmentFullName.indexOf('_');
        int secondUnderscore = departmentFullName.indexOf('_', firstUnderscore + 1);

        if (secondUnderscore == -1) {
            throw new IllegalArgumentException("Department name must contain an application name and department part.");
        }
        return departmentFullName.substring(0, secondUnderscore);
    }

    public static String formatDepartmentName(String applicationName, String departmentName) {
        return applicationName.toLowerCase().trim() + "_" + departmentName.toLowerCase().trim();
    }

    public static boolean validName(String input) {
        return input != null && input.matches(RESOURCE_NAME_REGEX);
    }

    public static String formatApplicationName(String applicationName) {
        return APPLICATION_PREFIX + applicationName.toLowerCase().trim();
    }
}
