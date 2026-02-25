package com.workcopilot.user.dto;

import com.workcopilot.user.entity.User;

public record UserInfoDto(
        Long id,
        String email,
        String name,
        String department,
        String position
) {
    public static UserInfoDto from(User user) {
        return new UserInfoDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getDepartment(),
                user.getPosition()
        );
    }
}
