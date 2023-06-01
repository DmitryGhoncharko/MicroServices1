package com.project.MicroServices.FirstService;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
class User {
    private final String username;
    private final String email;
    private final String password;
    private final String role;
}