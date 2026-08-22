package com.jobseekercopilot.jobmatching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobseekercopilot.jobmatching.client.LocationServiceClient;
import com.jobseekercopilot.jobmatching.dto.CanonicalLocation;
import com.jobseekercopilot.jobmatching.dto.CommuteAssessment;
import com.jobseekercopilot.jobmatching.dto.CommutePreferences;
import com.jobseekercopilot.jobmatching.dto.CommuteTravelMode;
import com.jobseekercopilot.jobmatching.dto.HomeLocation;
import com.jobseekercopilot.jobmatching.dto.JobMatchJob;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CommuteAssessmentServiceTest {
    private final LocationServiceClient locations = mock(LocationServiceClient.class);
    private final CommuteReferenceTimePolicy timePolicy = new CommuteReferenceTimePolicy(
            Clock.fixed(Instant.parse("2026-08-07T12:00:00Z"), ZoneOffset.UTC));
    private final CommuteAssessmentService service = new CommuteAssessmentService(
            locations, timePolicy, new SimpleMeterRegistry());

    @Test
    void remoteJobsDoNotConsumeRoutingBudget() {
        JobMatchJob job = job("remote", "REMOTE");

        service.assess(List.of(job), home(), preferences());

        assertThat(job.getCommuteAssessment().getStatus())
                .isEqualTo(CommuteAssessment.Status.NOT_APPLICABLE);
        verify(locations, never()).matrix(any());
    }

    @Test
    void evaluatesAtMostFiveDestinationsInOneBoundedMatrix() {
        List<JobMatchJob> jobs = new ArrayList<>();
        for (int index = 0; index < 6; index++) jobs.add(job("job-" + index, "ONSITE"));
        when(locations.matrix(any())).thenAnswer(invocation -> {
            LocationServiceClient.MatrixRequest request = invocation.getArgument(0);
            return new LocationServiceClient.MatrixResponse(request.destinations().stream()
                    .map(destination -> new LocationServiceClient.Estimate(
                            destination.referenceId(), CommuteTravelMode.DRIVE, "ESTIMATED", 35,
                            new BigDecimal("12.4"), request.departureTime(), null, "GOOGLE_MAPS"))
                    .toList());
        });

        service.assess(jobs, home(), preferences());

        ArgumentCaptor<LocationServiceClient.MatrixRequest> request =
                ArgumentCaptor.forClass(LocationServiceClient.MatrixRequest.class);
        verify(locations).matrix(request.capture());
        assertThat(request.getValue().destinations()).hasSize(5);
        assertThat(jobs.subList(0, 5)).allSatisfy(job -> assertThat(job.getCommuteAssessment().getStatus())
                .isEqualTo(CommuteAssessment.Status.WITHIN_PREFERENCE));
        assertThat(jobs.get(5).getCommuteAssessment().getExplanationCode()).isEqualTo("ROUTE_BUDGET");
    }

    @Test
    void providerFailureLeavesSearchResultsAvailable() {
        when(locations.matrix(any())).thenThrow(new LocationServiceClient.LocationServiceUnavailableException(
                new IllegalStateException("provider detail")));
        JobMatchJob job = job("job-1", "HYBRID");

        service.assess(List.of(job), home(), preferences());

        assertThat(job.getCommuteAssessment().getStatus())
                .isEqualTo(CommuteAssessment.Status.UNAVAILABLE);
        assertThat(job.getCommuteAssessment().getExplanationCode())
                .isEqualTo("ROUTING_PROVIDER_UNAVAILABLE");
    }

    private HomeLocation home() {
        HomeLocation home = new HomeLocation();
        home.setLocationId("home");
        home.setLatitude(new BigDecimal("51.5074"));
        home.setLongitude(new BigDecimal("-0.1278"));
        home.setPrecision("POSTCODE_CENTROID");
        return home;
    }

    private CommutePreferences preferences() {
        CommutePreferences preferences = new CommutePreferences();
        preferences.setCommuteTravelModes(List.of(CommuteTravelMode.DRIVE));
        preferences.setMaximumDrivingMinutes(45);
        return preferences;
    }

    private JobMatchJob job(String id, String workplaceType) {
        JobMatchJob job = new JobMatchJob();
        job.setId(id);
        job.setWorkplaceType(workplaceType);
        CanonicalLocation location = new CanonicalLocation();
        location.setLocationId("location-" + id);
        location.setLatitude(new BigDecimal("51.7520"));
        location.setLongitude(new BigDecimal("-1.2577"));
        location.setPrecision("POSTCODE_CENTROID");
        job.setCanonicalLocation(location);
        return job;
    }
}
