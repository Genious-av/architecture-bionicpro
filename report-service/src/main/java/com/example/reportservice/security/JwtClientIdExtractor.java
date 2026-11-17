
package com.example.reportservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;

@Component
public class JwtClientIdExtractor {

    private static final String CLIENT_ID_CLAIM = "i";

    private final ObjectMapper objectMapper;

    public JwtClientIdExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Long extractClientId(String authorizationHeader) {
        try {
            if (authorizationHeader == null || authorizationHeader.isBlank()) {
                throw new IllegalArgumentException("Authorization header is missing");
            }

            String token = authorizationHeader.trim();

            if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
                token = token.substring(7).trim();
            }

            String[] parts = token.split("\\.");

            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = objectMapper.readTree(payload);

            JsonNode clientIdClaim = claims.get(CLIENT_ID_CLAIM);

            if (clientIdClaim == null || clientIdClaim.isNull()) {
                throw new IllegalArgumentException("Client ID claim is missing");
            }

            return Long.parseLong(clientIdClaim.asText());

        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authorization token"
            );
        }
    }
}

