package com.expensetracker.controller;

import com.expensetracker.model.dto.response.UserProfileResponse;
import com.expensetracker.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/profile")
    public UserProfileResponse profile(@AuthenticationPrincipal UserPrincipal principal) {
        var user = principal.getUser();
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCurrency()
        );
    }
}
