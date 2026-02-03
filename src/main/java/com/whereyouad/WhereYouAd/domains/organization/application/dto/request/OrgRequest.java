package com.whereyouad.WhereYouAd.domains.organization.application.dto.request;

public class OrgRequest {

    public record Create (
            String name,
            String description,
            String logoUrl
    ) {}

    public record Read (
        //TODO
    ) {}

    public record Update (
        //TODO
    ) {}

    public record Delete (
        //TODO
    ) {}
}
