package com.cutting.cuttingsystem.entitys;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginInfo {
    private Long userId;
    private String username;
    private String realName;
    private String token;
}
