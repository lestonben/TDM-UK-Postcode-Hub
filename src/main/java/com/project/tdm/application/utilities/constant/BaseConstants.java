package com.project.tdm.application.utilities.constant;

public class BaseConstants {

    public static final String EMAIL_FORMAT = "^[A-Za-z0-9+_.-]+@(.+)$";

    public static final String JWT_TOKEN = "JWT_TOKEN";

    public static final String REDIS_KEY_PC_AUTOCOMPLETE = "postcodes:autocomplete";

    // -- Common Response Messages --
    public static final String LOGIN_SUCCESS_MSG = "Account login successful.";
    public static final String REGISTER_SUCCESS_MSG = "Account registration successful.";
    public static final String LOGOUT_SUCCESS_MSG = "Successfully logged out.";
    public static final String UNEXPECTED_ERROR_MSG = "An unexpected error occurred. Please try again.";
    public static final String INVALID_CREDENTIAL_MSG = "Invalid credentials. Please try again.";
    public static final String USERNAME_USED_MSG = "Username has been taken.";
    public static final String EMAIL_USED_MSG = "Email has been taken.";
}
