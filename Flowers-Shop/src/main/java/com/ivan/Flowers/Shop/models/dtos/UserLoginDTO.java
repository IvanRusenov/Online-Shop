package com.ivan.Flowers.Shop.models.dtos;

import jakarta.validation.constraints.Size;

public class UserLoginDTO {

    @Size(min = 2, max = 50, message = "Username length must be between 2 and 50 symbols!")
    private String username;

    @Size(min = 3, max = 20, message = "Password length must be between 3 and 20 symbols!")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}