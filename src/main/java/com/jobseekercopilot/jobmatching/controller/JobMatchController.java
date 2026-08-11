package com.jobseekercopilot.jobmatching.controller;

import com.jobseekercopilot.jobmatching.client.ApplicationTrackerClient;
import com.jobseekercopilot.jobmatching.dto.EnrichJobsRequest;
import com.jobseekercopilot.jobmatching.dto.EnrichJobsResponse;
import com.jobseekercopilot.jobmatching.service.JobMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/job-matches")
@Tag(name = "Job Matching", description = "Endpoints for enriching found jobs with application state")
public class JobMatchController {

    private final JobMatchingService jobMatchingService;

    public JobMatchController(JobMatchingService jobMatchingService) {
        this.jobMatchingService = jobMatchingService;
    }

    @PostMapping("/enrich")
    @Operation(summary = "Enrich job search results")
    public ResponseEntity<EnrichJobsResponse> enrichJobs(@RequestBody EnrichJobsRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(jobMatchingService.enrichJobs(
                request.getUserId(),
                request.getJobs(),
                request.getHomeLocation(),
                request.getCommutePreferences()));
    }

    @ExceptionHandler(ApplicationTrackerClient.ApplicationTrackerUnavailableException.class)
    public ResponseEntity<Void> handleApplicationTrackerUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
