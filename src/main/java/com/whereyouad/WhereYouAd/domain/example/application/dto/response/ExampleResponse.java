package com.whereyouad.WhereYouAd.domain.example.application.dto.response;

import com.whereyouad.WhereYouAd.domain.example.persistence.entity.ExampleEntity;

public record ExampleResponse(
        Long id,
        String name
) {
    public static ExampleResponse from(ExampleEntity example) {
        return new ExampleResponse(
                example.getId(),
                example.getName()
        );
    }
}

