package com.jobscheduler.job_scheduler.service;

import com.jobscheduler.job_scheduler.dto.request.CreateUserRequest;
import com.jobscheduler.job_scheduler.dto.response.UserResponse;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);
}