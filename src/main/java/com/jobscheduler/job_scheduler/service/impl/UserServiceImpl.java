package com.jobscheduler.job_scheduler.service.impl;

import com.jobscheduler.job_scheduler.dto.request.CreateUserRequest;
import com.jobscheduler.job_scheduler.dto.response.UserResponse;
import com.jobscheduler.job_scheduler.entity.User;
import com.jobscheduler.job_scheduler.entity.UserRole;
import com.jobscheduler.job_scheduler.exception.DuplicateResourceException;
import com.jobscheduler.job_scheduler.repository.UserRepository;
import com.jobscheduler.job_scheduler.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                    "User with email " + request.email() + " already exists"
            );
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException(
                    "User with username " + request.username() + " already exists"
            );
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .build();

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }
}