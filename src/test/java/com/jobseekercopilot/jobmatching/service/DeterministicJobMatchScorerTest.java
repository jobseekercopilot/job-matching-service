package com.jobseekercopilot.jobmatching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobseekercopilot.jobmatching.dto.CandidateProfile;
import com.jobseekercopilot.jobmatching.dto.CandidateQualification;
import com.jobseekercopilot.jobmatching.dto.CandidateRole;
import com.jobseekercopilot.jobmatching.dto.CommuteAssessment;
import com.jobseekercopilot.jobmatching.dto.JobDiscoveryAssessment;
import com.jobseekercopilot.jobmatching.dto.JobExperience;
import com.jobseekercopilot.jobmatching.dto.JobMatchJob;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeterministicJobMatchScorerTest {
    private final DeterministicJobMatchScorer scorer =
            new DeterministicJobMatchScorer(Clock.fixed(
                    Instant.parse("2026-08-12T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void producesProfileBasedScoreWithInspectableComponentsAndSkillGap() {
        JobMatchJob job = job("Junior Software Engineer",
                "Build Java, Angular and React services. At least 2 years experience.");
        job.getExperience().setMinimumYears(new BigDecimal("2"));
        CommuteAssessment commute = new CommuteAssessment();
        commute.setStatus(CommuteAssessment.Status.WITHIN_PREFERENCE);
        job.setCommuteAssessment(commute);
        CandidateProfile profile = profile(
                List.of("Java", "Angular"),
                List.of(role("Software Developer", "2021-01", "2024-12", "PREVIOUS_ROLE")));

        var result = scorer.assess(
                job, "Junior Software Engineer", profile, null);

        assertThat(result.getProvenance()).isEqualTo("DETERMINISTIC_PROFILE");
        assertThat(result.isCandidateProfileUsed()).isTrue();
        assertThat(result.getScore()).isBetween(.80, .95);
        assertThat(result.getComponents())
                .extracting(component -> component.getCode())
                .containsExactly(
                        "TARGET_ROLE_ALIGNMENT",
                        "ADVERTISED_SKILL_EVIDENCE",
                        "RELEVANT_EXPERIENCE",
                        "WORK_AND_COMMUTE_PREFERENCES");
        assertThat(result.getReasons())
                .anySatisfy(reason -> {
                    assertThat(reason.getCode()).isEqualTo("ADVERTISED_SKILL_GAPS");
                    assertThat(reason.getExplanation()).contains("react");
                });
    }

    @Test
    void queryOnlyScoreIsExplicitlyNotPersonalised() {
        JobMatchJob job = job("Junior Software Engineer", "Build software");

        var result = scorer.assess(
                job, "Junior Software Engineer", null, null);

        assertThat(result.getProvenance()).isEqualTo("DETERMINISTIC_QUERY_ONLY");
        assertThat(result.isCandidateProfileUsed()).isFalse();
        assertThat(result.getScore()).isEqualTo(1.0);
        assertThat(result.getReasons()).singleElement()
                .extracting(reason -> reason.getCode())
                .isEqualTo("PROFILE_EVIDENCE_NOT_SUPPLIED");
    }

    @Test
    void neverInfersEligibilityOrClearanceAndSurfacesExplicitGates() {
        JobMatchJob job = job("Junior Software Engineer", """
                Applicants must already have the right to work; no sponsorship.
                Security clearance is required. This is a new graduate role.
                A degree in computer science is required.
                """);
        CandidateQualification music = new CandidateQualification();
        music.setQualificationName("Master of Music");
        music.setStatus("COMPLETED");
        CandidateProfile profile = profile(List.of("Java"), List.of());
        profile.setQualifications(List.of(music));

        var result = scorer.assess(
                job, "Junior Software Engineer", profile, null);

        assertThat(result.getRating()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.getHardGateReasons())
                .extracting(reason -> reason.getCode())
                .contains(
                        "RIGHT_TO_WORK_UNVERIFIED",
                        "CLEARANCE_UNVERIFIED",
                        "NEW_GRAD_ELIGIBILITY_UNVERIFIED",
                        "COMPUTING_DEGREE_REQUIREMENT");
        assertThat(result.getHardGateReasons())
                .filteredOn(reason -> reason.getCode().endsWith("UNVERIFIED"))
                .allMatch(reason -> "UNVERIFIED".equals(reason.getStatus()));
        assertThat(result.getHardGateReasons())
                .filteredOn(reason -> reason.getCode().equals(
                        "COMPUTING_DEGREE_REQUIREMENT"))
                .allMatch(reason -> "UNVERIFIED".equals(reason.getStatus()));
    }

    @Test
    void mergesOverlappingRoleMonthsAndDoesNotInventMissingDays() {
        JobMatchJob job = job("Software Developer", "Four years experience required");
        job.getExperience().setMinimumYears(new BigDecimal("4"));
        CandidateProfile profile = profile(List.of("Java"), List.of(
                role("Software Engineer", "2020-01", "2021-12", "PREVIOUS_ROLE"),
                role("Software Developer", "2021-01", "2022-12", "PREVIOUS_ROLE")));

        var result = scorer.assess(job, "Software Developer", profile, null);

        assertThat(result.getHardGateReasons())
                .anySatisfy(reason -> {
                    assertThat(reason.getCode())
                            .isEqualTo("MINIMUM_EXPERIENCE_NOT_EVIDENCED");
                    assertThat(reason.getExplanation()).contains("3 years");
                });
    }

    @Test
    void identicalEvidenceProducesStableScoreAndExplanationOrder() {
        JobMatchJob first = job("Software Developer", "Java, SQL and Git");
        JobMatchJob second = job("Software Developer", "Java, SQL and Git");
        CandidateProfile profile = profile(List.of("Git", "Java"), List.of(
                role("Software Developer", "2022-01", "2025-01", "PREVIOUS_ROLE")));

        var firstResult = scorer.assess(first, "Software Developer", profile, null);
        var secondResult = scorer.assess(second, "Software Developer", profile, null);

        assertThat(secondResult.getScore()).isEqualTo(firstResult.getScore());
        assertThat(secondResult.getReasons())
                .extracting(reason -> reason.getCode())
                .containsExactlyElementsOf(firstResult.getReasons().stream()
                        .map(reason -> reason.getCode()).toList());
    }

    @Test
    void comparableJuniorSoftwareRoleAlwaysOutranksSeniorRoleForJuniorTarget() {
        JobMatchJob junior = job("Junior Software Developer", "Build Java services");
        JobMatchJob senior = job("Senior Software Developer", "Build Java services");
        senior.getDiscoveryAssessment().setSeniority("SENIOR");
        CandidateProfile profile = profile(
                List.of("Java"),
                List.of(role("Software Developer", "2025-01", "2026-07", "PREVIOUS")));

        var juniorAssessment = scorer.assess(
                junior, "Junior Software Developer", profile, null);
        var seniorAssessment = scorer.assess(
                senior, "Junior Software Developer", profile, null);

        assertThat(juniorAssessment.getScore())
                .isGreaterThan(seniorAssessment.getScore());
        assertThat(seniorAssessment.getReasons())
                .anySatisfy(reason -> {
                    assertThat(reason.getCode()).isEqualTo("SENIORITY_ABOVE_TARGET");
                    assertThat(reason.getStatus()).isEqualTo("NOT_MET");
                });
    }

    private JobMatchJob job(String title, String description) {
        JobMatchJob job = new JobMatchJob();
        job.setCanonicalJobId("fixture-1");
        job.setTitle(title);
        job.setJobTitle(title);
        job.setDescription(description);
        JobDiscoveryAssessment discovery = new JobDiscoveryAssessment();
        discovery.setTargetRoleAlignment("ALIGNED");
        discovery.setSeniority(title.toLowerCase().contains("junior")
                ? "JUNIOR_ENTRY" : "UNSPECIFIED");
        job.setDiscoveryAssessment(discovery);
        job.setExperience(new JobExperience());
        return job;
    }

    private CandidateProfile profile(
            List<String> skills,
            List<CandidateRole> roles) {
        CandidateProfile profile = new CandidateProfile();
        profile.setSkills(skills);
        profile.setRoles(roles);
        return profile;
    }

    private CandidateRole role(
            String title,
            String start,
            String end,
            String status) {
        CandidateRole role = new CandidateRole();
        role.setJobTitle(title);
        role.setStartDate(start);
        role.setEndDate(end);
        role.setStatus(status);
        return role;
    }
}
