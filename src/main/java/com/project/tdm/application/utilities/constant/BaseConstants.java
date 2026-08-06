package com.project.tdm.application.utilities.constant;

public class BaseConstants {

    // -- Formatting Rules --
    public static final String EMAIL_FORMAT = "^[A-Za-z0-9+_.-]+@(.+)$";


    // -- Default Key and Token Names --
    public static final String JWT_TOKEN = "JWT_TOKEN";
    public static final String REDIS_KEY_PC_AUTOCOMPLETE = "postcodes:autocomplete";

    // -- Common Response Messages --
    public static final String LOGIN_SUCCESS_MSG = "Account login successful.";
    public static final String REGISTER_SUCCESS_MSG = "Account registration successful.";
    public static final String LOGOUT_SUCCESS_MSG = "Successfully logged out.";
    public static final String CREATE_ROLE_SUCCESS_MSG = "A new role is created successful.";
    public static final String UPDATE_ROLE_SUCCESS_MSG = "A role is updated successful.";
    public static final String DELETE_ROLE_SUCCESS_MSG = "A role is deleted successful.";
    public static final String UPDATE_USER_ROLES_SUCCESS_MSG = "The user roles is updated successful.";
    public static final String UNEXPECTED_ERROR_MSG = "An unexpected error occurred. Please try again.";
    public static final String INVALID_CREDENTIAL_MSG = "Invalid credentials. Please try again.";
    public static final String USERNAME_USED_MSG = "Username has been taken.";
    public static final String EMAIL_USED_MSG = "Email has been taken.";
    public static final String DUPLICATE_ROLE_NAME_MSG = "Role name has been in use.";
    public static final String ROLE_RECORD_NOT_AVAILABLE = "The selected role is not available.";
    public static final String ONLY_ROLE_RECORD_CANNOT_DELETE = "The only 1 role and cannot be deleted";


    // -- Common Access Role Names & Descriptions --
    public static final String VIEWER_NAME = "VIEWER";
    public static final String VIEWER_DESC = "Has view access.";
    public static final String ADMINISTRATOR_NAME = "ADMINISTRATOR";
    public static final String ADMINISTRATOR_DESC = "Have full dashboard access.";


    // -- Request Endpoint Names and Url Patterns --
    public static final String DEFAULT_URL = "/";
    public static final String HOME_URL = "/tdm/home";
    public static final String DASHBOARD_MAIN_NAME = "Route Search";
    public static final String DASHBOARD_MAIN_URL = "/tdm/dashboard/main";
    public static final String DASHBOARD_UPDATE_NAME = "Search/Update";
    public static final String DASHBOARD_UPDATE_URL = "/tdm/dashboard/update";
    public static final String DASHBOARD_IMPORT_NAME = "Data Import";
    public static final String DASHBOARD_IMPORT_URL = "/tdm/dashboard/import";
    public static final String DASHBOARD_ROLE_MANAGEMENT_NAME = "Role Management";
    public static final String DASHBOARD_ROLE_MANAGEMENT_URL = "/tdm/dashboard/role-management";
    public static final String ERROR_PAGE_URL = "/tdm/access-denied";
}
