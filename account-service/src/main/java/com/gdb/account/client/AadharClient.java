package com.gdb.account.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for communicating with the Aadhar Verification Service (port 8005).
 * Called during savings account creation to validate the Aadhar number.
 */
@Component
@Slf4j
public class AadharClient {

    private final RestTemplate restTemplate;
    private final String aadharServiceUrl;
    private final ObjectMapper objectMapper;

    public AadharClient(RestTemplate restTemplate,
                        @Value("${external.aadhar-service.url}") String aadharServiceUrl,
                        ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.aadharServiceUrl = aadharServiceUrl;
        this.objectMapper = objectMapper;
    }

    /**
     * Response DTO for the Aadhar verification API.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AadharVerificationResponse {
        @JsonProperty("aadhar_number")
        private String aadharNumber;

        @JsonProperty("is_valid")
        private boolean isValid;

        private String status;
        private String message;
        private String timestamp;
    }

    /**
     * Verifies an Aadhar number by calling the external Aadhar Service.
     *
     * @param aadharNumber the 12-digit Aadhar number to verify
     * @return true if the Aadhar number is valid, false otherwise
     */
    @Retry(name = "aadharRetry", fallbackMethod = "verifyAadharFallback")
    public boolean verifyAadhar(String aadharNumber) {
        String url = aadharServiceUrl + "/api/v1/verify";
        log.info("Calling Aadhar Service at {} for verification", url);

        try {
            Map<String, String> requestBody = Map.of("aadhar_number", aadharNumber);
            AadharVerificationResponse response = restTemplate.postForObject(
                    url, requestBody, AadharVerificationResponse.class);

            if (response != null) {
                log.info("Aadhar verification result for {}: {} - {}",
                        maskAadhar(aadharNumber), response.getStatus(), response.getMessage());
                return response.isValid();
            }

            log.warn("Aadhar Service returned null response");
            return false;

        } catch (HttpClientErrorException e) {
            // TODO: MOD11-BUG-01: Swallowed client validation error.
            // Trainee task: When the Aadhar service rejects an Aadhar number due to formatting errors,
            // it throws a 400 Bad Request exception, which hits this generic catch block and shows
            // "Aadhar verification service is unavailable".
            // Add a specific catch block for HttpClientErrorException to parse the response body
            // and throw the specific validation error message instead of the generic outage message.
            log.error("Client error from Aadhar Service (HTTP {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            try {
                AadharVerificationResponse errorResponse = objectMapper.readValue(
                        e.getResponseBodyAsString(), AadharVerificationResponse.class);
                if (errorResponse != null && errorResponse.getMessage() != null) {
                    // Propagate the exact message (e.g. "Aadhar number not found in registry")
                    throw new RuntimeException(errorResponse.getMessage());
                }
            } catch (Exception parseException) {
                log.error("Failed to parse Aadhar error response body", parseException);
            }
            throw new RuntimeException("Aadhar validation failed: " + e.getStatusText());

        } catch (Exception e) {
            // TODO: MOD11-CR-01: Resilience4j Retry & Fallback.
            // Trainee task: Introduce a retry configuration on this client call.
            // Add Resilience4j dependency to pom.xml and use @Retry with a fallback method.
            log.error("Error calling Aadhar Service: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback method executed when all Resilience4j retry attempts are exhausted.
     * Keeps the onboarding wizard active and prevents systemic service crashes.
     */
    public boolean verifyAadharFallback(String aadharNumber, Exception exception) {
        log.error("Aadhar service tracking streams exhausted. Fallback triggered. Cause: {}", exception.getMessage());

        // If it's a specific validation error from the HTTP 400 catch block, bypass the fallback message and bubble it up
        if (exception instanceof RuntimeException && !exception.getMessage().contains("unavailable")) {
            throw (RuntimeException) exception;
        }

        throw new RuntimeException("Aadhar verification service is unavailable. Please try again later.");
    }

    /**
     * Masks an Aadhar number for logging (showing only first 4 digits).
     */
    private String maskAadhar(String aadharNumber) {
        if (aadharNumber == null || aadharNumber.length() < 4) {
            return "XXXXXXXXXXXX";
        }
        return aadharNumber.substring(0, 4) + "XXXXXXXX";
    }
}