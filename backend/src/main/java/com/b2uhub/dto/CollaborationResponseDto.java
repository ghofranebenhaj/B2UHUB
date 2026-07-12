package com.b2uhub.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CollaborationResponseDto {
    private String agent;
    private String message;
    private String methode;
    private Boolean llmActive;
    private List<CollaborationOpportunityDto> opportunites;
}
