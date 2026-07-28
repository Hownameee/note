package com.example.authdemo.services;

import com.example.authdemo.dtos.UserResponse;
import java.util.List;

public interface UserService {

    List<UserResponse> getUsers();

    UserResponse getUser(Long userId);

    UserResponse saveGitHubLogin(String username, String githubToken);
}
