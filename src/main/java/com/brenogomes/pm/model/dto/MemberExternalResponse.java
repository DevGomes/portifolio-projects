package com.brenogomes.pm.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MemberExternalResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
}
