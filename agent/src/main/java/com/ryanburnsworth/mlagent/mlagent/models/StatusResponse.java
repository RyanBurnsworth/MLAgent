package com.ryanburnsworth.mlagent.mlagent.models;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StatusResponse {
    String status;

    String message;

    String details;
}
