package com.erik.git_bro.service.github;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubAppTokenService {



    // got that here: https://github.com/settings/installations/71819645
    private final String INSTALLATION_ID = "71819645"; // TODO - Remove becuase this is my personal installation ID used for testing.
    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${github.app.id}")
    private String appId;

    @Value("${gitbro.app.private-key}")
    private String privatePem; // point to your PEM file

    @Value("${gitbro.app.public-key}")
    private String publicPem; // point to your Public PEM file

    /**
     * Load the GitHub App private key from PEM file.
     * Pem Files are already base64 encoded. So you need to take what is end between the BEGIN and END. Decode that. 
     * I included stripping those, just in case I need to re-up it and forget to manually remove them.
     * To get this to work, you need both a private and public key. The private key needs to be downloaded from GitHub.
     * 
     * The private key downloaded from GitHub is Not PKCS#1, it's PKCS#8. To have a private key compatible with Java, 
     * it's needs to be #1. The following OpenSSL command converts one to the other:
     * openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in my_rsa_github_pem.pem -out private_key_pkcs8.pem
     * 
     * However, that is not all. Take the orignal RSA key, and run the follwoing command to retreive the public key.
     * openssl rsa -in gitbro-ai-platform.2025-06-17.private-key.pem -pubout -out gitbro-public-key.pem      
     * 
     * Now you should have a valid public and private key. Both of these are needed to build the jwt, which is done with Nimbus

     */
    public RSAPrivateKey loadPrivateKey() throws Exception {
        // Strip the header and footer
        privatePem = privatePem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", ""); // Remove all whitespace/newlines

        // Decode base64 to DER bytes
        byte[] keyBytes = Base64.getDecoder().decode(privatePem);
        log.info("=======");
        // Generate RSA private key
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(keySpec);
    }

    public RSAPublicKey loadPublicKey() throws Exception {
    

        // Strip the header and footer
        this.publicPem = this.publicPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", ""); // Remove all whitespace/newlines

        // Decode base64 to DER bytes
        byte[] keyBytes = Base64.getDecoder().decode(this.publicPem);

        // Generate RSA public key
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(keySpec);
    }

    public String createJwtToken() throws Exception {
        RSAPrivateKey privateKey = loadPrivateKey();
        RSAPublicKey publicKey = loadPublicKey();

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(540); // 9 minutes

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(appId)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();

        // Build RSA JWK
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) publicKey)
                .privateKey(privateKey)
                .keyID("gitbro-key") // any unique key ID
                .build();

        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(rsaKey));
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(jwkSource);

        JwtEncoderParameters params = JwtEncoderParameters.from(claims);
        Jwt jwt = encoder.encode(params);

        log.info("Generated GitHub App JWT: {}", jwt.getTokenValue());
        return jwt.getTokenValue();
    }

      /**
     * Exchange the app JWT for an installation access token. This is for my own
     * app.
     * 
     * @throws Exception
     */
    public String getInstallationToken() throws Exception {
        final String jwt = createJwtToken();

        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/app/installations/" + INSTALLATION_ID + "/access_tokens"))
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to get the installation token: " + response.body());

        }
        final JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.get("token").asText();
    }

    /**
     * Exchange the app JWT for an installation access token
     * 
     * @throws Exception
     */
    public String getInstallationId(final String owner, final String repo) throws Exception {
        final String jwt = createJwtToken();

        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/installation"))
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get the installation ID: " + response.body());

        }
        final JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.get("id").asText();
    }

    public String getInstallationToken(final long installationId) throws Exception {
        final String jwt = createJwtToken();

        final HttpClient client = HttpClient.newHttpClient();

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/app/installations/" + installationId + "/access_tokens"))
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to get installation token: " + response.body());
        }

        final JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.get("token").asText();
    }

}
