package com.example.authdemo.mappers;

import com.example.authdemo.dtos.UserResponse;
import com.example.authdemo.entities.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getUserId(), user.getUsername());
    }
}
