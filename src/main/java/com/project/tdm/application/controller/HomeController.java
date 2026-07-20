package com.project.tdm.application.controller;

import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.service.UserService;

import com.project.tdm.application.util.BaseConstants;
import com.project.tdm.security.util.CookieUtil;
import com.project.tdm.security.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@RestController
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private CookieUtil cookieUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public Object redirectLogin(HttpServletRequest request, HttpServletResponse response) {
        String tokenStr = cookieUtil.searchCookieValue(request, BaseConstants.JWT_TOKEN);

        if (tokenStr != null && !("").equals(tokenStr)) {
            boolean isTokenExpired = jwtUtil.isTokenExpired(tokenStr);
            if (!isTokenExpired) {
                // Token is valid: Redirect them away from home into the dashboard area
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, "/tdm/dashboard/main")
                        .build();
            }
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/tdm/home")
                .build();
    }

    @RequestMapping(value = "/api/login", method = RequestMethod.POST)
    public ResponseEntity<?> login(@RequestBody Map<String, String> paramsMap, HttpServletResponse response) {
        String usernameEmail = paramsMap.get("usernameEmail");
        String password = paramsMap.get("password");
        logger.info("login(): received authentication request from user = " + usernameEmail);

        UserEntity user = new UserEntity();
        Predicate<String> isEmail = (input -> input.matches(BaseConstants.EMAIL_FORMAT));
        Optional.of(usernameEmail)
                .filter(isEmail)
                .ifPresentOrElse(user::setEmail, () -> user.setUsername(usernameEmail));
        user.setPassword(password);

        try {
            // 1. validate user credentials
            UserEntity verifiedUser = userService.loginUser(user);
            logger.info("login(): successfully logged in by the user = " + verifiedUser.getUsername());

            // 2. assign JWT
            String token = jwtUtil.generateToken(verifiedUser);
            logger.info("login(): generated JWT string");

            // 3. set cookie
            Cookie cookie = cookieUtil.setCookieAlive(BaseConstants.JWT_TOKEN, token);
            response.addCookie(cookie);
            logger.info("login(): set JWT string into cookie");

            return ResponseEntity.ok("");
        }
        catch (IllegalArgumentException ex) {
            logger.warn("login(): failed by user = {}, errorMessage = {}", usernameEmail, ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        catch (Exception ex) {
            logger.error("login(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body("An unexpected error occurred. Please try again.");
        }
    }

    @RequestMapping(value = "/api/register", method = RequestMethod.POST)
    public ResponseEntity<?> register(@RequestBody Map<String, String> paramsMap) {
        String username = paramsMap.get("username");
        String email = paramsMap.get("email");
        String password = paramsMap.get("password");
        logger.info("register(): received registration request from user = " + username + ", email = " + email);

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);

        try {
            userService.registerUser(user);
            logger.info("register(): successfully registered account.");

            return ResponseEntity.ok("Account registration successful.");
        }
        catch (IllegalArgumentException ex) {
            logger.warn("register(): failed by user = {}, errorMessage = {}", username, ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        catch (Exception ex) {
            logger.error("register(): unexpected error occurred by user = " + username, ex);
            return ResponseEntity.internalServerError().body("An unexpected error occurred. Please try again.");
        }
    }

    @RequestMapping(value = "/api/logout", method = RequestMethod.POST)
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = cookieUtil.setCookieExpired(BaseConstants.JWT_TOKEN);
        response.addCookie(cookie);

        return ResponseEntity.ok().body(Map.of("message", "Successfully logged out."));
     }
}
