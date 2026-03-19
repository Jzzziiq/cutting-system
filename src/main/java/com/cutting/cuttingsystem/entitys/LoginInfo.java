package com.cutting.cuttingsystem.entitys;

import lombok.Data;

@Data
public class LoginInfo {
    private Long userId;
    private String username;
    private String realName;
    private String token;
}
