package com.jobseekercopilot.jobmatching.service;

import com.jobseekercopilot.jobmatching.client.ApplicationTrackerClient;
import com.jobseekercopilot.jobmatching.dto.ApplicationRecord;
import com.jobseekercopilot.jobmatching.dto.ApplicationStatus;
import com.jobseekercopilot.jobmatching.dto.EnrichJobsResponse;
import com.jobseekercopilot.jobmatching.dto.JobMatchJob;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class JobMatchingService {

    private static final Logger log = LoggerFactory.getLogger(JobMatchingService.class);

    private static final String DEFAULT_PROVIDER = "REED";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final ApplicationTrackerClient applicationTrackerClient;

    public JobMatchingService(ApplicationTrackerClient applicationTrackerClient) {
        this.applicationTrackerClient = applicationTrackerClient;
    }

    public EnrichJobsResponse enrichJobs(String userId, List<JobMatchJob> jobs) {
        long startedAt = System.nanoTime();
        List<JobMatchJob> safeJobs = jobs == null ? List.of() : jobs;
        log.info("Job match enrich request userId={} jobsReceived={}", userId, safeJobs.size());
        List<ApplicationRecord> applicationRecords = applicationTrackerClient.getApplicationsForUser(userId);
        MatchCounters counters = new MatchCounters();
        List<JobMatchJob> enrichedJobs = safeJobs.stream()
                .map(job -> enrichJob(job, applicationRecords, counters))
                .toList();
        log.info("Job match enrich complete userId={} jobsReceived={} applicationRecordsLoaded={} "
                        + "matchesByCanonicalId={} matchesByProviderExternalId={} fallbackMatches={} unmatched={} durationMs={}",
                userId,
                safeJobs.size(),
                applicationRecords.size(),
                counters.canonicalMatches,
                counters.providerExternalMatches,
                counters.fallbackMatches,
                counters.unmatched,
                (System.nanoTime() - startedAt) / 1_000_000);
        return new EnrichJobsResponse(userId, enrichedJobs);
    }

    public Optional<ApplicationRecord> findMatchingApplication(JobMatchJob job, List<ApplicationRecord> applicationRecords) {
        if (job == null || applicationRecords == null) {
            return Optional.empty();
        }

        return applicationRecords.stream()
                .sorted(Comparator.comparing(ApplicationRecord::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .filter(record -> matchByCanonicalJobId(job, record))
                .findFirst()
                .or(() -> applicationRecords.stream()
                        .sorted(Comparator.comparing(ApplicationRecord::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .filter(record -> matchByProviderAndExternalJobId(job, record))
                        .findFirst())
                .or(() -> applicationRecords.stream()
                        .sorted(Comparator.comparing(ApplicationRecord::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .filter(record -> matchByNormalisedFields(job, record))
                        .findFirst());
    }

    public boolean matchByCanonicalJobId(JobMatchJob job, ApplicationRecord record) {
        String jobCanonicalId = firstNonBlank(job.getCanonicalJobId(), job.getId());
        String recordCanonicalId = firstNonBlank(record.getCanonicalJobId(), record.getJobId());
        return !isBlank(jobCanonicalId)
                && !isBlank(recordCanonicalId)
                && normalise(jobCanonicalId).equals(normalise(recordCanonicalId));
    }

    public boolean matchByProviderAndExternalJobId(JobMatchJob job, ApplicationRecord record) {
        String jobProvider = provider(job.getProvider());
        String recordProvider = provider(record.getProvider());
        String jobExternalId = firstNonBlank(job.getExternalJobId(), job.getId());
        String recordExternalId = firstNonBlank(record.getExternalJobId(), record.getJobId());

        if (!isBlank(jobExternalId)
                && !isBlank(recordExternalId)
                && normalise(jobProvider).equals(normalise(recordProvider))
                && normalise(jobExternalId).equals(normalise(recordExternalId))) {
            return true;
        }

        if (job.getSources() == null) {
            return false;
        }
        return job.getSources().stream().anyMatch(source ->
                !isBlank(source.getExternalJobId())
                        && !isBlank(recordExternalId)
                        && normalise(provider(source.getProvider())).equals(normalise(recordProvider))
                        && normalise(source.getExternalJobId()).equals(normalise(recordExternalId)));
    }

    public boolean matchByNormalisedFields(JobMatchJob job, ApplicationRecord record) {
        return fieldMatches(firstNonBlank(job.getJobTitle(), job.getTitle()), record.getJobTitle())
                && fieldMatches(firstNonBlank(job.getCompanyName(), job.getCompany()), record.getCompanyName())
                && fieldMatches(job.getLocation(), record.getLocation());
    }

    public String normalise(String value) {
        if (value == null) {
            return "";
        }
        String noDiacritics = DIACRITICS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
        String noPunctuation = PUNCTUATION.matcher(noDiacritics).replaceAll(" ");
        return WHITESPACE.matcher(noPunctuation.toLowerCase().trim()).replaceAll(" ");
    }

    private JobMatchJob enrichJob(JobMatchJob job, List<ApplicationRecord> applicationRecords, MatchCounters counters) {
        MatchingApplication matchingApplication = findMatchingApplicationWithStrategy(job, applicationRecords);
        if (matchingApplication.record().isPresent()) {
            switch (matchingApplication.strategy()) {
                case CANONICAL -> counters.canonicalMatches++;
                case PROVIDER_EXTERNAL -> counters.providerExternalMatches++;
                case FALLBACK -> counters.fallbackMatches++;
                default -> counters.unmatched++;
            }
            applyMatch(job, matchingApplication.record().get());
        } else {
            counters.unmatched++;
            markNew(job);
        }
        return job;
    }

    private MatchingApplication findMatchingApplicationWithStrategy(JobMatchJob job, List<ApplicationRecord> applicationRecords) {
        if (job == null || applicationRecords == null) {
            return new MatchingApplication(Optional.empty(), MatchStrategy.NONE);
        }

        List<ApplicationRecord> sortedRecords = applicationRecords.stream()
                .sorted(Comparator.comparing(ApplicationRecord::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        Optional<ApplicationRecord> canonicalMatch = sortedRecords.stream()
                .filter(record -> matchByCanonicalJobId(job, record))
                .findFirst();
        if (canonicalMatch.isPresent()) {
            return new MatchingApplication(canonicalMatch, MatchStrategy.CANONICAL);
        }
        Optional<ApplicationRecord> providerExternalMatch = sortedRecords.stream()
                .filter(record -> matchByProviderAndExternalJobId(job, record))
                .findFirst();
        if (providerExternalMatch.isPresent()) {
            return new MatchingApplication(providerExternalMatch, MatchStrategy.PROVIDER_EXTERNAL);
        }
        return new MatchingApplication(sortedRecords.stream()
                .filter(record -> matchByNormalisedFields(job, record))
                .findFirst(), MatchStrategy.FALLBACK);
    }

    private void applyMatch(JobMatchJob job, ApplicationRecord record) {
        job.setProvider(provider(job.getProvider()));
        job.setExternalJobId(firstNonBlank(job.getExternalJobId(), job.getId()));
        job.setCanonicalJobId(firstNonBlank(job.getCanonicalJobId(), record.getCanonicalJobId(), record.getJobId()));
        job.setId(firstNonBlank(job.getCanonicalJobId(), job.getId()));
        job.setApplicationStatus(record.getStatus() == null ? ApplicationStatus.DOCUMENTS_GENERATED : record.getStatus());
        job.setApplicationId(record.getId());
        job.setCvDocumentId(record.getCvDocumentId());
        job.setCoverLetterDocumentId(record.getCoverLetterDocumentId());
        job.setAppliedAt(asUtcTimestamp(record.getAppliedAt()));
        job.setApplicationUpdatedAt(asUtcTimestamp(record.getUpdatedAt()));
    }

    private void markNew(JobMatchJob job) {
        job.setProvider(provider(job.getProvider()));
        job.setExternalJobId(firstNonBlank(job.getExternalJobId(), job.getId()));
        job.setCanonicalJobId(firstNonBlank(job.getCanonicalJobId(), job.getId()));
        job.setId(firstNonBlank(job.getCanonicalJobId(), job.getId()));
        job.setApplicationStatus(ApplicationStatus.NEW);
        job.setApplicationId(null);
        job.setCvDocumentId(null);
        job.setCoverLetterDocumentId(null);
        job.setAppliedAt(null);
        job.setApplicationUpdatedAt(null);
    }

    private OffsetDateTime asUtcTimestamp(LocalDateTime timestamp) {
        return timestamp == null ? null : timestamp.atOffset(ZoneOffset.UTC);
    }

    private boolean fieldMatches(String left, String right) {
        String normalisedLeft = normalise(left);
        String normalisedRight = normalise(right);
        return !normalisedLeft.isBlank() && normalisedLeft.equals(normalisedRight);
    }

    private String provider(String value) {
        return isBlank(value) ? DEFAULT_PROVIDER : value;
    }

    private String firstNonBlank(String preferred, String fallback) {
        return isBlank(preferred) ? fallback : preferred;
    }

    private String firstNonBlank(String first, String second, String third) {
        if (!isBlank(first)) {
            return first;
        }
        if (!isBlank(second)) {
            return second;
        }
        return third;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private enum MatchStrategy {
        CANONICAL,
        PROVIDER_EXTERNAL,
        FALLBACK,
        NONE
    }

    private record MatchingApplication(Optional<ApplicationRecord> record, MatchStrategy strategy) {
    }

    private static class MatchCounters {
        private int canonicalMatches;
        private int providerExternalMatches;
        private int fallbackMatches;
        private int unmatched;
    }
}
