package com.project.tdm.application.controller;

import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.service.UserService;

import com.project.tdm.application.util.BaseConstants;
import com.project.tdm.security.util.CookieUtil;
import com.project.tdm.security.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@RestController
public class HomeController {

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

        UserEntity user = new UserEntity();
        Predicate<String> isEmail = (input -> input.matches(BaseConstants.EMAIL_FORMAT));
        Optional.of(usernameEmail)
                .filter(isEmail)
                .ifPresentOrElse(user::setEmail, () -> user.setUsername(usernameEmail));
        user.setPassword(password);

        try {
            // 1. validate user credentials
            UserEntity verifiedUser = userService.loginUser(user);

            // 2. assign JWT
            String token = jwtUtil.generateToken(verifiedUser);

            // 3. set cookie
            Cookie cookie = cookieUtil.setCookieAlive(BaseConstants.JWT_TOKEN, token);
            response.addCookie(cookie);

            // 4. set token into local storage
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put(BaseConstants.JWT_TOKEN, token);

            return ResponseEntity.ok(responseMap);
        }
        catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        catch (Exception ex) {
            return ResponseEntity.internalServerError().body("An unexpected error occurred. Please try again.");
        }
    }

    @RequestMapping(value = "/api/register", method = RequestMethod.POST)
    public ResponseEntity<?> register(@RequestBody Map<String, String> paramsMap) {
        UserEntity user = new UserEntity();
        user.setUsername(paramsMap.get("username"));
        user.setEmail(paramsMap.get("email"));
        user.setPassword(paramsMap.get("password"));

        try {
            userService.registerUser(user);

            return ResponseEntity.ok("Account registration successful.");
        }
        catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        catch (Exception ex) {
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
