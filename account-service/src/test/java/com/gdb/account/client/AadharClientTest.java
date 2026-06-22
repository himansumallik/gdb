package com.gdb.account.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

// TODO: MOD9-BUG-01: Unit test failure due to missing Mockito Extension runner.
// Trainee task: Run this test. It throws a NullPointerException because Mockito mocks
// are not initialized. Find which class-level runner/extension annotation is missing.
// Hint: JUnit 5 uses @ExtendWith to configure runners.
@ExtendWith(MockitoExtension.class)
public class AadharClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AadharClient aadharClient;

    @Test
    public void testVerifyAadhar_Success() {
        // Redacted identification placeholder used for simulation
        String testIdentifier = "123456789012";

        AadharClient.AadharVerificationResponse mockResponse = new AadharClient.AadharVerificationResponse(
                testIdentifier, true, "SUCCESS", "Valid", "2026-06-16T12:00:00"
        );

        // Every parameter now uses a strict Mockito argument matcher
        Mockito.when(restTemplate.postForObject(
                Mockito.anyString(),
                Mockito.anyMap(),
                Mockito.eq(AadharClient.AadharVerificationResponse.class)
        )).thenReturn(mockResponse);

        boolean result = aadharClient.verifyAadhar(testIdentifier);
        assertTrue(result);
    }
}
