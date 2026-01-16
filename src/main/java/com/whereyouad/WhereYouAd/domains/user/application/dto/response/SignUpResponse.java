package com.whereyouad.WhereYouAd.domains.user.application.dto.response;

import java.time.LocalDateTime;

public record SignUpResponse(
         Long userId,
         LocalDateTime createdAt
) { }
