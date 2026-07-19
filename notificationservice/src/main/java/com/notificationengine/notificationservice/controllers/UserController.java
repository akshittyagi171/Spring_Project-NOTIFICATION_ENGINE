package com.notificationengine.notificationservice.controllers;

import com.notificationengine.notificationservice.models.dtos.APIResponse;
import com.notificationengine.notificationservice.models.dtos.PreferenceUpdateRequest;
import com.notificationengine.notificationservice.models.dtos.UserCreateRequest;
import com.notificationengine.notificationservice.models.dtos.UserCreateResponse;
import com.notificationengine.notificationservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/sync")
    public ResponseEntity<APIResponse<UserCreateResponse>> syncUser(@RequestBody UserCreateRequest userCreateRequest) {
        if (userCreateRequest.getEmail() == null || userCreateRequest.getEmail().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.failure(400, "Validation Failed", "Email cannot be empty or null"));
        }

        UserCreateResponse response = userService.createOrFindUser(userCreateRequest);

        if ("CREATED".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(APIResponse.success(201, "User profile registered successfully", response));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(200, "User profile matched and synced", response));
    }

    @PutMapping("/update")
    public ResponseEntity<APIResponse<UserCreateResponse>> updateProfile(@RequestBody UserCreateRequest userCreateRequest) {
        if (userCreateRequest.getEmail() == null || userCreateRequest.getEmail().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.failure(400, "Validation Failed", "Email is mandatory for profile modification mappings"));
        }

        try {
            UserCreateResponse response = userService.updateUserDetails(userCreateRequest);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(APIResponse.success(200, "User profile and tokens synchronized successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.failure(404, "Not Found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.failure(500, "Internal Server Error", "Failed to update profile due to an internal pipeline error"));
        }
    }

    @PutMapping("/{userId}/preferences")
    public ResponseEntity<APIResponse<String>> updatePreferences(
            @PathVariable Long userId,
            @RequestBody List<PreferenceUpdateRequest> updateRequests) {

        log.info("Received preference update request for User ID: {}", userId);

        userService.updateUserPreferences(userId, updateRequests);

        return ResponseEntity.status(HttpStatus.OK)
                .body(APIResponse.success(200, "User preferences updated successfully", "SUCCESS"));
    }
}