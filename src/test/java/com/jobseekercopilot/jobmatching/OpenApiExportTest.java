package com.jobseekercopilot.jobmatching;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiExportTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void exportOpenApi() throws Exception {
        String spec = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var contract = new ObjectMapper().readTree(spec);
        assertThat(contract.at("/info/version").asText()).isEqualTo("1.1.0");
        assertThat(spec)
                .contains("candidateProfile", "matchAssessment",
                        "hardGateReasons", "discoveryAssessment");
        assertThat(contract.at("/components/schemas/MatchAssessment/properties/provenance").isObject())
                .isTrue();
        assertThat(contract.at("/components/schemas/JobDiscoveryAssessment/properties/seniority").isObject())
                .isTrue();
        Files.writeString(Path.of("target/openapi.json"), spec);
    }
}
