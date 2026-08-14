package com.jobseekercopilot.jobmatching.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI jobMatchingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Job Seeker Copilot - Job Matching Service API")
                .description("Application-state enrichment plus deterministic, evidence-based job matching. No LLM is used for ranking.")
                .version("1.1.0"));
    }
}
