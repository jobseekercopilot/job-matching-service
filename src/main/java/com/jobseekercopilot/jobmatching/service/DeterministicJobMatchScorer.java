package com.jobseekercopilot.jobmatching.service;

import com.jobseekercopilot.jobmatching.dto.CandidateProfile;
import com.jobseekercopilot.jobmatching.dto.CandidateQualification;
import com.jobseekercopilot.jobmatching.dto.CandidateRole;
import com.jobseekercopilot.jobmatching.dto.CommuteAssessment;
import com.jobseekercopilot.jobmatching.dto.CommutePreferences;
import com.jobseekercopilot.jobmatching.dto.JobDiscoveryAssessment;
import com.jobseekercopilot.jobmatching.dto.JobMatchJob;
import com.jobseekercopilot.jobmatching.dto.JobSkill;
import com.jobseekercopilot.jobmatching.dto.MatchAssessment;
import com.jobseekercopilot.jobmatching.dto.MatchReason;
import com.jobseekercopilot.jobmatching.dto.MatchScoreComponent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Profile-aware, bounded and inspectable ranking. It never calls an LLM. */
@Component
public class DeterministicJobMatchScorer {
    static final String ALGORITHM_VERSION = "PROFILE_MATCH_V1";
    private static final double TITLE_WEIGHT = .35;
    private static final double SKILL_WEIGHT = .30;
    private static final double EXPERIENCE_WEIGHT = .20;
    private static final double PREFERENCE_WEIGHT = .15;
    private static final Set<String> TECHNOLOGIES = Set.of(
            "java", "javascript", "typescript", "angular", "react", "vue",
            "python", "c#", ".net", "spring", "spring boot", "sql",
            "postgresql", "mysql", "aws", "azure", "gcp", "docker",
            "kubernetes", "git", "rest", "graphql", "node.js", "html", "css",
            "sass", "scss", "jenkins", "terraform", "linux", "go", "ruby");
    private static final Pattern RIGHT_TO_WORK = Pattern.compile(
            "(?i)(must (?:already )?have (?:the )?right to work|no (?:visa )?sponsorship|unable to (?:offer|provide) sponsorship|must be eligible to work)");
    private static final Pattern CLEARANCE = Pattern.compile(
            "(?i)(security clearance|sc cleared|developed vetting|must (?:hold|obtain) clearance)");
    private static final Pattern NEW_GRAD = Pattern.compile(
            "(?i)(new grad(?:uate)?|recent graduate|graduat(?:e|ing|ed) (?:in|between) 20\\d{2})");
    private static final Pattern DEGREE = Pattern.compile(
            "(?i)(must have (?:a )?degree|degree (?:is )?required|bachelor(?:'s)? degree|bsc|msc)");
    private static final Pattern COMPUTING_DEGREE = Pattern.compile(
            "(?i)(computer science|software engineering|computing|information technology).{0,40}degree|degree.{0,40}(computer science|software engineering|computing|information technology)");
    private static final Pattern DEGREE_EVIDENCE = Pattern.compile(
            "(?i)\\b(ba|bsc|ma|msc|bachelor|master|degree)\\b");

    private final Clock clock;

    public DeterministicJobMatchScorer() {
        this(Clock.systemUTC());
    }

    DeterministicJobMatchScorer(Clock clock) {
        this.clock = clock;
    }

    void score(
            List<JobMatchJob> jobs,
            String targetRole,
            CandidateProfile candidateProfile,
            CommutePreferences preferences) {
        if (jobs == null) return;
        jobs.forEach(job -> apply(job, targetRole, candidateProfile, preferences));
    }

    MatchAssessment assess(
            JobMatchJob job,
            String targetRole,
            CandidateProfile candidateProfile,
            CommutePreferences preferences) {
        targetRole = effectiveTargetRole(job, targetRole);
        boolean profileUsed = hasCandidateEvidence(candidateProfile);
        List<MatchScoreComponent> components = new ArrayList<>();
        List<MatchReason> reasons = new ArrayList<>();
        List<MatchReason> hardGates = hardGates(job, candidateProfile);

        double titleRatio = titleRatio(job, targetRole);
        if (!profileUsed) {
            double score = round(titleRatio);
            components.add(new MatchScoreComponent(
                    "TARGET_ROLE_ALIGNMENT", score, 1.0,
                    titleExplanation(job, targetRole)));
            reasons.add(reason(
                    "PROFILE_EVIDENCE_NOT_SUPPLIED", "INFO", "UNVERIFIED",
                    "This is query-title relevance only; it is not a personal match score."));
            return assessment(score, false, targetRole, components, reasons, hardGates);
        }

        double titleScore = round(titleRatio * TITLE_WEIGHT);
        components.add(new MatchScoreComponent(
                "TARGET_ROLE_ALIGNMENT", titleScore, TITLE_WEIGHT,
                titleExplanation(job, targetRole)));

        SkillResult skills = skillResult(job, candidateProfile);
        double skillScore = round(skills.ratio() * SKILL_WEIGHT);
        components.add(new MatchScoreComponent(
                "ADVERTISED_SKILL_EVIDENCE", skillScore, SKILL_WEIGHT,
                skills.explanation()));
        if (!skills.matched().isEmpty()) {
            reasons.add(reason("SKILLS_EVIDENCED", "POSITIVE", "MET",
                    "Profile evidence matches: " + String.join(", ", skills.matched()) + "."));
        }
        if (!skills.missing().isEmpty()) {
            reasons.add(reason("ADVERTISED_SKILL_GAPS", "GAP", "NOT_MET",
                    "No profile evidence was found for: "
                            + String.join(", ", skills.missing().stream().limit(5).toList()) + "."));
        }

        ExperienceResult experience = experienceResult(job, targetRole, candidateProfile);
        double experienceScore = round(experience.ratio() * EXPERIENCE_WEIGHT);
        components.add(new MatchScoreComponent(
                "RELEVANT_EXPERIENCE", experienceScore, EXPERIENCE_WEIGHT,
                experience.explanation()));
        reasons.addAll(experience.reasons());
        if (experience.hardGate() != null) {
            hardGates.add(experience.hardGate());
        }

        PreferenceResult preference = preferenceResult(job, preferences);
        double preferenceScore = round(preference.ratio() * PREFERENCE_WEIGHT);
        components.add(new MatchScoreComponent(
                "WORK_AND_COMMUTE_PREFERENCES", preferenceScore,
                PREFERENCE_WEIGHT, preference.explanation()));
        if (preference.reason() != null) reasons.add(preference.reason());

        JobDiscoveryAssessment discovery = job == null ? null : job.getDiscoveryAssessment();
        if (discovery != null
                && Set.of("SENIOR", "LEADERSHIP").contains(discovery.getSeniority())
                && isJuniorTarget(targetRole)) {
            reasons.add(reason("SENIORITY_ABOVE_TARGET", "GAP", "NOT_MET",
                    "The advert is classified as " + discovery.getSeniority()
                            + " while the selected target role is junior or entry level."));
        }

        double total = round(titleScore + skillScore + experienceScore + preferenceScore);
        return assessment(total, true, targetRole, components, reasons, hardGates);
    }

    private void apply(
            JobMatchJob job,
            String targetRole,
            CandidateProfile candidateProfile,
            CommutePreferences preferences) {
        MatchAssessment assessment = assess(job, targetRole, candidateProfile, preferences);
        job.setMatchAssessment(assessment);
        job.setMatchScore(assessment.getScore());
    }

    private String effectiveTargetRole(JobMatchJob job, String requestedTargetRole) {
        if (requestedTargetRole != null && !requestedTargetRole.isBlank()) {
            return requestedTargetRole;
        }
        if (job != null && job.getDiscoveryAssessment() != null
                && job.getDiscoveryAssessment().getTargetRole() != null
                && !job.getDiscoveryAssessment().getTargetRole().isBlank()) {
            return job.getDiscoveryAssessment().getTargetRole();
        }
        return firstNonBlank(job == null ? null : job.getTitle(),
                job == null ? null : job.getJobTitle());
    }

    private MatchAssessment assessment(
            double score,
            boolean profileUsed,
            String targetRole,
            List<MatchScoreComponent> components,
            List<MatchReason> reasons,
            List<MatchReason> hardGates) {
        MatchAssessment result = new MatchAssessment();
        result.setScore(score);
        result.setProvenance(profileUsed
                ? "DETERMINISTIC_PROFILE"
                : "DETERMINISTIC_QUERY_ONLY");
        result.setAlgorithmVersion(ALGORITHM_VERSION);
        result.setTargetRole(targetRole);
        result.setCandidateProfileUsed(profileUsed);
        result.setComponents(List.copyOf(components));
        result.setReasons(List.copyOf(reasons));
        result.setHardGateReasons(List.copyOf(hardGates));
        boolean unresolvedGate = hardGates.stream()
                .anyMatch(gate -> !"MET".equals(gate.getStatus()));
        result.setRating(unresolvedGate
                ? "REVIEW_REQUIRED"
                : score >= .80 ? "STRONG"
                : score >= .65 ? "GOOD"
                : score >= .45 ? "PARTIAL" : "WEAK");
        return result;
    }

    private double titleRatio(JobMatchJob job, String targetRole) {
        JobDiscoveryAssessment discovery = job == null ? null : job.getDiscoveryAssessment();
        if (discovery != null) {
            double alignment = switch (String.valueOf(discovery.getTargetRoleAlignment())) {
                case "ALIGNED" -> 1.0;
                case "RELATED" -> .65;
                case "MISMATCHED" -> 0.0;
                default -> .30;
            };
            // Occupation tokens alone make senior and junior adverts look
            // identical. Bound the senior title contribution for a junior
            // target, keeping the advert visible but below a comparable
            // genuine junior vacancy.
            if (isJuniorTarget(targetRole)
                    && Set.of("SENIOR", "LEADERSHIP")
                            .contains(discovery.getSeniority())) {
                return Math.min(alignment, .35);
            }
            return alignment;
        }
        Set<String> target = coreTokens(targetRole);
        Set<String> title = coreTokens(firstNonBlank(
                job == null ? null : job.getTitle(),
                job == null ? null : job.getJobTitle()));
        if (target.isEmpty() || title.isEmpty()) return .20;
        long matching = target.stream().filter(title::contains).count();
        return Math.min(1.0, (double) matching / target.size());
    }

    private String titleExplanation(JobMatchJob job, String targetRole) {
        String title = firstNonBlank(job == null ? null : job.getTitle(), job == null ? null : job.getJobTitle());
        String alignment = job == null || job.getDiscoveryAssessment() == null
                ? "token comparison"
                : String.valueOf(job.getDiscoveryAssessment().getTargetRoleAlignment()).toLowerCase(Locale.ROOT);
        return "Advert title '" + safe(title) + "' is " + alignment
                + " with target role '" + safe(targetRole) + "'.";
    }

    private SkillResult skillResult(JobMatchJob job, CandidateProfile profile) {
        Set<String> candidate = profile.getSkills().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(this::canonicalSkill)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> advertised = advertisedSkills(job);
        if (advertised.isEmpty()) {
            return new SkillResult(.5, List.of(), List.of(),
                    "The advert contains no recognised technology terms, so this component is neutral.");
        }
        List<String> matched = advertised.stream().filter(candidate::contains).sorted().toList();
        List<String> missing = advertised.stream().filter(skill -> !candidate.contains(skill)).sorted().toList();
        double ratio = (double) matched.size() / advertised.size();
        return new SkillResult(ratio, matched, missing,
                matched.size() + " of " + advertised.size()
                        + " recognised advertised technologies are evidenced in the profile.");
    }

    private Set<String> advertisedSkills(JobMatchJob job) {
        LinkedHashSet<String> skills = new LinkedHashSet<>();
        if (job != null && job.getSkills() != null) {
            job.getSkills().stream()
                    .map(JobSkill::getName)
                    .filter(value -> value != null && !value.isBlank())
                    .map(this::canonicalSkill)
                    .forEach(skills::add);
        }
        String searchable = normalize(firstNonBlank(job == null ? null : job.getTitle(), "")
                + " " + (job == null || job.getDescription() == null ? "" : job.getDescription()));
        TECHNOLOGIES.stream()
                .filter(skill -> containsPhrase(searchable, skill))
                .map(this::canonicalSkill)
                .sorted()
                .forEach(skills::add);
        return skills;
    }

    private ExperienceResult experienceResult(
            JobMatchJob job,
            String targetRole,
            CandidateProfile profile) {
        double years = relevantExperienceYears(profile, targetRole);
        BigDecimal required = job == null || job.getExperience() == null
                ? null : job.getExperience().getMinimumYears();
        List<MatchReason> reasons = new ArrayList<>();
        if (required != null && required.signum() > 0) {
            double requiredYears = required.doubleValue();
            double ratio = Math.min(1.0, years / requiredYears);
            MatchReason gate = years + .001 < requiredYears
                    ? reason("MINIMUM_EXPERIENCE_NOT_EVIDENCED", "GAP", "NOT_MET",
                            formatYears(years) + " years of relevant dated role evidence is available; the advert states at least "
                                    + formatYears(requiredYears) + " years.")
                    : null;
            if (gate == null) {
                reasons.add(reason("MINIMUM_EXPERIENCE_EVIDENCED", "POSITIVE", "MET",
                        "Dated relevant roles meet the advert's stated minimum experience."));
            }
            return new ExperienceResult(ratio,
                    formatYears(years) + " relevant years compared with a stated minimum of "
                            + formatYears(requiredYears) + ".", reasons, gate);
        }
        String seniority = job == null || job.getDiscoveryAssessment() == null
                ? "UNSPECIFIED" : job.getDiscoveryAssessment().getSeniority();
        double expected = switch (String.valueOf(seniority)) {
            case "LEADERSHIP" -> 7.0;
            case "SENIOR" -> 5.0;
            case "MID" -> 2.0;
            case "JUNIOR_ENTRY" -> 1.0;
            default -> 1.0;
        };
        double ratio = years <= 0 ? .20 : Math.min(1.0, years / expected);
        reasons.add(reason("RELEVANT_DATED_EXPERIENCE", years > 0 ? "POSITIVE" : "INFO",
                years > 0 ? "MET" : "UNVERIFIED",
                formatYears(years) + " years were calculated from explicitly stored relevant role months."));
        return new ExperienceResult(ratio,
                "Relevant experience is calculated from stored role months; no missing day was invented.",
                reasons, null);
    }

    private double relevantExperienceYears(CandidateProfile profile, String targetRole) {
        if (profile == null || profile.getRoles() == null) return 0;
        String targetFamily = occupationFamily(targetRole);
        List<MonthRange> ranges = profile.getRoles().stream()
                .filter(java.util.Objects::nonNull)
                .filter(role -> relevantRole(targetFamily, role.getJobTitle()))
                .map(this::range)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(MonthRange::start))
                .toList();
        if (ranges.isEmpty()) return 0;
        List<MonthRange> merged = new ArrayList<>();
        for (MonthRange range : ranges) {
            if (merged.isEmpty() || range.start() > merged.get(merged.size() - 1).end() + 1) {
                merged.add(range);
            } else {
                MonthRange previous = merged.remove(merged.size() - 1);
                merged.add(new MonthRange(previous.start(), Math.max(previous.end(), range.end())));
            }
        }
        int months = merged.stream().mapToInt(range -> range.end() - range.start() + 1).sum();
        return Math.round((months / 12.0) * 10.0) / 10.0;
    }

    private MonthRange range(CandidateRole role) {
        YearMonth start = parseMonth(role.getStartDate());
        YearMonth end = parseMonth(role.getEndDate());
        if (end == null && "CURRENT".equalsIgnoreCase(safe(role.getStatus()))) {
            end = YearMonth.now(clock);
        }
        if (start == null || end == null || end.isBefore(start)) return null;
        return new MonthRange(monthIndex(start), monthIndex(end));
    }

    private YearMonth parseMonth(String value) {
        if (value == null || value.length() < 7) return null;
        try {
            return YearMonth.parse(value.substring(0, 7));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private int monthIndex(YearMonth value) {
        return value.getYear() * 12 + value.getMonthValue() - 1;
    }

    private PreferenceResult preferenceResult(
            JobMatchJob job,
            CommutePreferences preferences) {
        CommuteAssessment assessment = job == null ? null : job.getCommuteAssessment();
        if (assessment != null && assessment.getStatus() != null) {
            return switch (assessment.getStatus()) {
                case WITHIN_PREFERENCE, NOT_APPLICABLE -> new PreferenceResult(
                        1.0, "The commute assessment is within the stored preference.",
                        reason("COMMUTE_WITHIN_PREFERENCE", "POSITIVE", "MET",
                                "The available commute assessment is within preference."));
                case ABOVE_PREFERENCE -> new PreferenceResult(
                        .20, "The commute assessment is above the stored preference.",
                        reason("COMMUTE_ABOVE_PREFERENCE", "GAP", "NOT_MET",
                                "The available commute assessment exceeds the stored preference."));
                default -> new PreferenceResult(
                        .50, "Commute evidence is unavailable or not evaluated.",
                        reason("COMMUTE_NOT_VERIFIED", "INFO", "UNVERIFIED",
                                "No verified commute outcome is available."));
            };
        }
        String workplace = job == null ? null : job.getWorkplaceType();
        List<String> arrangements = preferences == null
                ? List.of() : preferences.getWorkplaceArrangements();
        if (workplace != null && arrangements != null && !arrangements.isEmpty()) {
            boolean match = arrangements.stream().filter(java.util.Objects::nonNull)
                    .map(this::normalize).anyMatch(value -> value.equals(normalize(workplace))
                            || (value.equals("onsite") && normalize(workplace).equals("on site")));
            return new PreferenceResult(match ? 1.0 : .25,
                    match ? "Workplace type matches a stored arrangement preference."
                            : "Workplace type does not match the stored arrangement preferences.",
                    reason("WORKPLACE_PREFERENCE", match ? "POSITIVE" : "GAP",
                            match ? "MET" : "NOT_MET",
                            match ? "The workplace arrangement is preferred."
                                    : "The workplace arrangement is not among the stored preferences."));
        }
        return new PreferenceResult(.50,
                "No comparable work or commute preference is available.", null);
    }

    private List<MatchReason> hardGates(JobMatchJob job, CandidateProfile profile) {
        String description = job == null || job.getDescription() == null ? "" : job.getDescription();
        List<MatchReason> gates = new ArrayList<>();
        if (RIGHT_TO_WORK.matcher(description).find()) {
            gates.add(reason("RIGHT_TO_WORK_UNVERIFIED", "INFO", "UNVERIFIED",
                    "The advert states a right-to-work or sponsorship condition; the profile contract contains no eligibility assertion."));
        }
        if (CLEARANCE.matcher(description).find()) {
            gates.add(reason("CLEARANCE_UNVERIFIED", "INFO", "UNVERIFIED",
                    "The advert mentions security clearance; no clearance eligibility was inferred."));
        }
        if (NEW_GRAD.matcher(description).find()) {
            gates.add(reason("NEW_GRAD_ELIGIBILITY_UNVERIFIED", "INFO", "UNVERIFIED",
                    "The advert includes a new-graduate condition that requires a claimant answer."));
        }
        boolean equivalentExperience = normalize(description).contains("or equivalent experience");
        if (!equivalentExperience && COMPUTING_DEGREE.matcher(description).find()) {
            boolean evidenced = qualificationNames(profile).stream().anyMatch(name ->
                    containsAny(normalize(name), Set.of(
                            "computer science", "software engineering", "computing", "information technology")));
            gates.add(reason("COMPUTING_DEGREE_REQUIREMENT", evidenced ? "POSITIVE" : "INFO",
                    "UNVERIFIED",
                    evidenced
                            ? "A stored qualification name appears related to the specified computing subject; the requirement still needs human verification."
                            : "The specified computing-degree requirement is not verified by the supplied qualification names."));
        } else if (!equivalentExperience && DEGREE.matcher(description).find()) {
            boolean evidenced = qualificationNames(profile).stream()
                    .anyMatch(name -> DEGREE_EVIDENCE.matcher(name).find());
            gates.add(reason("DEGREE_REQUIREMENT", evidenced ? "POSITIVE" : "INFO",
                    "UNVERIFIED",
                    evidenced ? "A stored qualification name appears to evidence a degree; the advert's exact requirement still needs human verification."
                            : "The advert states a degree condition that is not verified by the supplied qualification names."));
        }
        return gates;
    }

    private List<String> qualificationNames(CandidateProfile profile) {
        if (profile == null || profile.getQualifications() == null) return List.of();
        return profile.getQualifications().stream()
                .filter(java.util.Objects::nonNull)
                .map(CandidateQualification::getQualificationName)
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private boolean hasCandidateEvidence(CandidateProfile profile) {
        return profile != null && ((profile.getSkills() != null && !profile.getSkills().isEmpty())
                || (profile.getRoles() != null && !profile.getRoles().isEmpty())
                || (profile.getQualifications() != null && !profile.getQualifications().isEmpty()));
    }

    private boolean relevantRole(String targetFamily, String roleTitle) {
        String roleFamily = occupationFamily(roleTitle);
        if (targetFamily.equals(roleFamily) && !"UNKNOWN".equals(targetFamily)) return true;
        return Set.of("SOFTWARE", "DATA", "QUALITY_ENGINEERING", "CYBER_SECURITY").contains(targetFamily)
                && Set.of("SOFTWARE", "DATA", "QUALITY_ENGINEERING", "CYBER_SECURITY").contains(roleFamily);
    }

    private String occupationFamily(String value) {
        String text = normalize(value);
        if (containsAny(text, Set.of("software", "developer", "programmer", "frontend", "front end", "backend", "back end", "full stack", "devops", "platform engineer", "site reliability", "web engineer", "cloud engineer"))) return "SOFTWARE";
        if (containsAny(text, Set.of("data scientist", "data analyst", "data engineer", "machine learning"))) return "DATA";
        if (containsAny(text, Set.of("quality assurance", "qa engineer", "test engineer", "software tester"))) return "QUALITY_ENGINEERING";
        if (containsAny(text, Set.of("cyber", "security engineer", "penetration", "soc analyst"))) return "CYBER_SECURITY";
        return "UNKNOWN";
    }

    private boolean isJuniorTarget(String targetRole) {
        String target = normalize(targetRole);
        return containsAny(target, Set.of("junior", "graduate", "entry level", "trainee", "apprentice"));
    }

    private String canonicalSkill(String value) {
        String skill = normalize(value);
        return switch (skill) {
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "dotnet", "net" -> ".net";
            case "c sharp" -> "c#";
            case "postgres", "postgre sql" -> "postgresql";
            case "node", "nodejs" -> "node.js";
            case "amazon web services" -> "aws";
            case "google cloud", "google cloud platform" -> "gcp";
            default -> skill;
        };
    }

    private boolean containsPhrase(String searchable, String phrase) {
        String normalizedPhrase = normalize(phrase);
        return Pattern.compile("(^|[^a-z0-9])" + Pattern.quote(normalizedPhrase)
                + "([^a-z0-9]|$)").matcher(searchable).find();
    }

    private Set<String> coreTokens(String value) {
        Set<String> ignored = Set.of("junior", "senior", "lead", "principal", "graduate", "entry", "level", "role", "job");
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        Arrays.stream(normalize(value).split(" "))
                .filter(token -> token.length() > 1 && !ignored.contains(token))
                .forEach(tokens::add);
        return tokens;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#.]+", " ").trim().replaceAll("\\s+", " ");
    }

    private boolean containsAny(String value, Set<String> phrases) {
        return phrases.stream().anyMatch(value::contains);
    }

    private MatchReason reason(String code, String severity, String status, String explanation) {
        return new MatchReason(code, severity, status, explanation);
    }

    private double round(double value) {
        return BigDecimal.valueOf(Math.max(0, Math.min(1, value)))
                .setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatYears(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private String safe(String value) { return value == null ? "unknown" : value.trim(); }
    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private record SkillResult(double ratio, List<String> matched, List<String> missing, String explanation) {}
    private record ExperienceResult(double ratio, String explanation, List<MatchReason> reasons, MatchReason hardGate) {}
    private record PreferenceResult(double ratio, String explanation, MatchReason reason) {}
    private record MonthRange(int start, int end) {}
}
