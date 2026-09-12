package com.jobscheduler.job_scheduler.service;

import com.jobscheduler.job_scheduler.dto.request.CreateUserRequest;
import com.jobscheduler.job_scheduler.dto.request.LoginRequest;
import com.jobscheduler.job_scheduler.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(
            CreateUserRequest request
    );

    AuthResponse login(
            LoginRequest request
    );
}
