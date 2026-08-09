package com.jobseekercopilot.jobmatching.service;

import com.jobseekercopilot.jobmatching.client.LocationServiceClient;
import com.jobseekercopilot.jobmatching.dto.CanonicalLocation;
import com.jobseekercopilot.jobmatching.dto.CommuteAssessment;
import com.jobseekercopilot.jobmatching.dto.CommutePreferences;
import com.jobseekercopilot.jobmatching.dto.CommuteTravelMode;
import com.jobseekercopilot.jobmatching.dto.HomeLocation;
import com.jobseekercopilot.jobmatching.dto.JobMatchJob;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CommuteAssessmentService {
    private static final int MAX_DESTINATIONS = 5;
    private final LocationServiceClient locations;
    private final CommuteReferenceTimePolicy timePolicy;
    private final MeterRegistry meters;

    public CommuteAssessmentService(
            LocationServiceClient locations,
            CommuteReferenceTimePolicy timePolicy,
            MeterRegistry meters) {
        this.locations = locations;
        this.timePolicy = timePolicy;
        this.meters = meters;
    }

    public void assess(List<JobMatchJob> jobs, HomeLocation origin, CommutePreferences preferences) {
        List<JobMatchJob> safeJobs = jobs == null ? List.of() : jobs;
        CommutePreferences safePreferences = preferences == null ? new CommutePreferences() : preferences;
        List<CommuteTravelMode> modes = safePreferences.getCommuteTravelModes();

        safeJobs.stream().filter(this::remote).forEach(job ->
                job.setCommuteAssessment(base(job, CommuteAssessment.Status.NOT_APPLICABLE, "REMOTE_NO_COMMUTE")));

        if (modes == null || modes.isEmpty()) {
            safeJobs.stream().filter(job -> job.getCommuteAssessment() == null).forEach(job ->
                    job.setCommuteAssessment(base(job, CommuteAssessment.Status.NOT_EVALUATED, "NO_COMMUTE_PREFERENCE")));
            return;
        }
        if (!routable(origin)) {
            safeJobs.stream().filter(job -> job.getCommuteAssessment() == null).forEach(job ->
                    job.setCommuteAssessment(base(job, CommuteAssessment.Status.UNAVAILABLE, "ORIGIN_UNAVAILABLE")));
            return;
        }

        List<JobMatchJob> candidates = new ArrayList<>();
        for (JobMatchJob job : safeJobs) {
            if (job.getCommuteAssessment() != null) continue;
            if (unknownWorkplace(job)) {
                job.setCommuteAssessment(base(job, CommuteAssessment.Status.UNAVAILABLE, "WORKPLACE_TYPE_UNKNOWN"));
            } else if (!routable(job.getCanonicalLocation())) {
                job.setCommuteAssessment(base(job, CommuteAssessment.Status.UNAVAILABLE, "DESTINATION_UNAVAILABLE"));
            } else if (candidates.size() < MAX_DESTINATIONS) {
                candidates.add(job);
            } else {
                job.setCommuteAssessment(base(job, CommuteAssessment.Status.NOT_EVALUATED, "ROUTE_BUDGET"));
            }
        }
        meters.counter("job.matching.commute.shortlist", "outcome", "eligible").increment(candidates.size());
        if (candidates.isEmpty()) return;

        var request = new LocationServiceClient.MatrixRequest(
                new LocationServiceClient.RoutePoint(
                        reference(origin.getLocationId(), "origin"), origin.getLatitude(), origin.getLongitude(),
                        precision(origin.getPrecision())),
                candidates.stream().map(job -> new LocationServiceClient.RoutePoint(
                        identity(job), job.getCanonicalLocation().getLatitude(), job.getCanonicalLocation().getLongitude(),
                        precision(job.getCanonicalLocation().getPrecision()))).toList(),
                modes, timePolicy.nextWorkdayDeparture());
        try {
            LocationServiceClient.MatrixResponse response = locations.matrix(request);
            apply(candidates, response, safePreferences, origin);
        } catch (LocationServiceClient.LocationServiceUnavailableException exception) {
            candidates.forEach(job -> job.setCommuteAssessment(
                    base(job, CommuteAssessment.Status.UNAVAILABLE, "ROUTING_PROVIDER_UNAVAILABLE")));
            meters.counter("job.matching.commute.requests", "outcome", "unavailable").increment();
        }
    }

    private void apply(
            List<JobMatchJob> candidates,
            LocationServiceClient.MatrixResponse response,
            CommutePreferences preferences,
            HomeLocation origin) {
        Map<String, List<LocationServiceClient.Estimate>> byDestination = new LinkedHashMap<>();
        if (response != null && response.estimates() != null) {
            response.estimates().forEach(value -> byDestination
                    .computeIfAbsent(value.destinationReferenceId(), ignored -> new ArrayList<>()).add(value));
        }
        for (JobMatchJob job : candidates) {
            List<LocationServiceClient.Estimate> estimates = byDestination.getOrDefault(identity(job), List.of());
            CommuteAssessment assessment = base(job, CommuteAssessment.Status.UNAVAILABLE, "NO_ROUTE_ESTIMATE");
            List<CommuteAssessment.ModeAssessment> modes = estimates.stream()
                    .map(value -> mode(value, preferences, origin, job.getCanonicalLocation())).toList();
            assessment.setModes(modes);
            List<CommuteAssessment.ModeAssessment> successful = modes.stream()
                    .filter(value -> value.getEstimateStatus() != CommuteAssessment.EstimateStatus.UNAVAILABLE).toList();
            if (!successful.isEmpty()) {
                boolean within = successful.stream().anyMatch(value -> value.getOutcome() == CommuteAssessment.Outcome.WITHIN);
                assessment.setStatus(within ? CommuteAssessment.Status.WITHIN_PREFERENCE
                        : CommuteAssessment.Status.ABOVE_PREFERENCE);
                assessment.setExplanationCode(within ? "COMMUTE_WITHIN_PREFERENCE" : "COMMUTE_ABOVE_PREFERENCE");
                assessment.setBestSuitableMode(successful.stream()
                        .filter(value -> value.getOutcome() == CommuteAssessment.Outcome.WITHIN)
                        .min(Comparator.comparing(CommuteAssessment.ModeAssessment::getDurationMinutes))
                        .map(CommuteAssessment.ModeAssessment::getMode).orElse(null));
                assessment.setProviderAttribution("GOOGLE_MAPS");
            }
            job.setCommuteAssessment(assessment);
        }
        meters.counter("job.matching.commute.requests", "outcome", "success").increment();
    }

    private CommuteAssessment.ModeAssessment mode(
            LocationServiceClient.Estimate estimate,
            CommutePreferences preferences,
            HomeLocation origin,
            CanonicalLocation destination) {
        CommuteAssessment.ModeAssessment result = new CommuteAssessment.ModeAssessment();
        result.setMode(estimate.mode());
        result.setEstimateStatus(parseStatus(estimate.status()));
        result.setDurationMinutes(estimate.durationMinutes());
        result.setDistanceMiles(estimate.distanceMiles());
        Integer threshold = estimate.mode() == CommuteTravelMode.DRIVE
                ? preferences.getMaximumDrivingMinutes() : preferences.getMaximumTransitMinutes();
        result.setThresholdMinutes(threshold);
        result.setOutcome(estimate.durationMinutes() == null ? CommuteAssessment.Outcome.UNAVAILABLE
                : threshold == null ? CommuteAssessment.Outcome.NO_PREFERENCE
                : estimate.durationMinutes() <= threshold ? CommuteAssessment.Outcome.WITHIN
                : CommuteAssessment.Outcome.ABOVE);
        result.setCalculatedFor(estimate.calculatedFor() == null ? null
                : OffsetDateTime.ofInstant(estimate.calculatedFor(), ZoneOffset.UTC));
        result.setOriginPrecision(precision(origin.getPrecision()));
        result.setDestinationPrecision(precision(destination.getPrecision()));
        result.setReasonCode(estimate.reasonCode());
        return result;
    }

    private CommuteAssessment.EstimateStatus parseStatus(String status) {
        try { return CommuteAssessment.EstimateStatus.valueOf(status); }
        catch (RuntimeException exception) { return CommuteAssessment.EstimateStatus.UNAVAILABLE; }
    }

    private CommuteAssessment base(JobMatchJob job, CommuteAssessment.Status status, String code) {
        CommuteAssessment assessment = new CommuteAssessment();
        assessment.setStatus(status);
        assessment.setWorkplaceType(workplace(job));
        assessment.setExplanationCode(code);
        return assessment;
    }

    private boolean remote(JobMatchJob job) { return "REMOTE".equals(workplace(job)); }
    private boolean unknownWorkplace(JobMatchJob job) { return "UNKNOWN".equals(workplace(job)); }
    private String workplace(JobMatchJob job) {
        return job == null || job.getWorkplaceType() == null ? "UNKNOWN" : job.getWorkplaceType();
    }
    private boolean routable(HomeLocation value) {
        return value != null && value.getLatitude() != null && value.getLongitude() != null
                && !"NONE".equals(precision(value.getPrecision()));
    }
    private boolean routable(CanonicalLocation value) {
        return value != null && value.getLatitude() != null && value.getLongitude() != null
                && !"NONE".equals(precision(value.getPrecision()));
    }
    private String precision(String value) { return value == null || value.isBlank() ? "PROVIDER_COORDINATE" : value; }
    private String identity(JobMatchJob job) { return reference(job.getCanonicalJobId(), reference(job.getId(), job.getExternalJobId())); }
    private String reference(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
