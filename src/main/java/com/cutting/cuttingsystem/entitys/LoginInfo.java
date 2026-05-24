package com.cutting.cuttingsystem.entitys;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginInfo {
    private Long userId;
    private String username;
    private String realName;
    private String token;
    private List<String> roles;
    private List<String> permissions;
    private Long orgId;
    private String orgRole;
    private String orgName;
}
