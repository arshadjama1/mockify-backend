package com.mockify.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "mockify.rate-limit")
@Getter
@Setter
//map YAML properties into this class so the rate limiter can read limits dynamically.
public class RateLimitProperties {


    // Store global rate limit props
    private Limit global;

    // Store group based rate limit props
    /*
        Key = group name (auth, records, admin etc.)
        Value = configuration for that group.
    */
    private Map<String, GroupLimit> groups;

    @Getter
    @Setter
    public static class Limit {
        private int limit;
        private Duration window;
        private String type;
    }

    @Getter
    @Setter
    public static class GroupLimit extends Limit {
        private List<String> paths;
    }
}
