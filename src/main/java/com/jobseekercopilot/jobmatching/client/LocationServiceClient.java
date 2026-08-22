package com.jobseekercopilot.jobmatching.client;

import com.jobseekercopilot.generated.locationservice.api.CommuteApi;
import com.jobseekercopilot.generated.locationservice.client.ApiClient;
import com.jobseekercopilot.generated.locationservice.model.Precision;
import com.jobseekercopilot.generated.locationservice.model.TravelMode;
import com.jobseekercopilot.jobmatching.dto.CommuteTravelMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class LocationServiceClient {
    private final CommuteApi commuteApi;

    public LocationServiceClient(
            RestTemplateBuilder builder,
            @Value("${services.location-service.base-url:http://localhost:8104}") String baseUrl,
            @Value("${services.location-service.service-token}") String serviceToken,
            @Value("${services.location-service.connect-timeout:500ms}") java.time.Duration connectTimeout,
            @Value("${services.location-service.read-timeout:4s}") java.time.Duration readTimeout) {
        if (serviceToken == null || serviceToken.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("LOCATION_SERVICE_TOKEN must contain at least 32 bytes");
        }
        ApiClient client = new ApiClient(
                builder.setConnectTimeout(connectTimeout).setReadTimeout(readTimeout).build())
                .setBasePath(baseUrl);
        client.setApiKey(serviceToken);
        this.commuteApi = new CommuteApi(client);
    }

    public MatrixResponse matrix(MatrixRequest request) {
        try {
            var response = commuteApi.estimateCommutes(
                    new com.jobseekercopilot.generated.locationservice.model.CommuteMatrixRequest()
                            .origin(point(request.origin()))
                            .destinations(request.destinations().stream().map(this::point).toList())
                            .modes(request.modes().stream()
                                    .map(mode -> TravelMode.fromValue(mode.name()))
                                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
                            .departureTime(OffsetDateTime.ofInstant(request.departureTime(), ZoneOffset.UTC)));
            return new MatrixResponse(response == null || response.getEstimates() == null
                    ? List.of()
                    : response.getEstimates().stream().map(value -> new Estimate(
                            value.getDestinationReferenceId(),
                            CommuteTravelMode.valueOf(value.getMode().getValue()),
                            value.getStatus().getValue(),
                            value.getDurationMinutes(),
                            decimal(value.getDistanceMiles()),
                            value.getCalculatedFor() == null ? null : value.getCalculatedFor().toInstant(),
                            value.getReasonCode(),
                            value.getProviderAttribution() == null
                                    ? null : value.getProviderAttribution().getValue())).toList());
        } catch (RestClientException exception) {
            throw new LocationServiceUnavailableException(exception);
        }
    }

    private com.jobseekercopilot.generated.locationservice.model.RoutePoint point(RoutePoint value) {
        return new com.jobseekercopilot.generated.locationservice.model.RoutePoint()
                .referenceId(value.referenceId())
                .latitude(value.latitude().doubleValue())
                .longitude(value.longitude().doubleValue())
                .precision(Precision.fromValue(value.precision()));
    }

    private BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    public record MatrixRequest(
            RoutePoint origin,
            List<RoutePoint> destinations,
            List<CommuteTravelMode> modes,
            Instant departureTime) { }
    public record RoutePoint(
            String referenceId, BigDecimal latitude, BigDecimal longitude, String precision) { }
    public record MatrixResponse(List<Estimate> estimates) { }
    public record Estimate(
            String destinationReferenceId,
            CommuteTravelMode mode,
            String status,
            Integer durationMinutes,
            BigDecimal distanceMiles,
            Instant calculatedFor,
            String reasonCode,
            String providerAttribution) { }

    public static class LocationServiceUnavailableException extends RuntimeException {
        public LocationServiceUnavailableException(Throwable cause) { super(cause); }
    }
}
