/**
 * Copyright (c) 2016-2017 in alphabetical order:
 * Bosch Software Innovations GmbH, Robert Bosch GmbH, Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Denis Kramer     (Bosch Software Innovations GmbH)
 *    Stefan Schmid    (Robert Bosch GmbH)
 *    Andreas Ziller   (Siemens AG)
 */
package org.eclipse.bridgeiot.lib.security;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.Base64Variants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

public class AccessToken {

    private static final Logger logger = LoggerFactory.getLogger(AccessToken.class);
    private static final String JWT_SECRET = "123456789012345678901234567890";
    private static final long TOKEN_LIFETIME = 3600000;

    // JWT signature algorithm we will be using to sign the token
    private static SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    private AccessToken() {
    }

    public static String generate(String consumerId, String offeringId) {

        return generate(consumerId, offeringId, JWT_SECRET);
    }

    public static String generate(String consumerId, String offeringId, final String secret) {

        // final byte[] secretBase64Encoded = Base64.getEncoder().encode(secret.getBytes());
        final String secretBase64Encoded = Base64Variants.getDefaultVariant().encode(secret.getBytes());

        return Jwts.builder().setId(consumerId).setIssuedAt(getCurrentDate()).setSubject(offeringId)
                .setIssuer(consumerId).signWith(signatureAlgorithm, secretBase64Encoded)
                .setExpiration(getExpirationDate(TOKEN_LIFETIME)).compact();

        // logger.info("JWT Token: {}", jwtToken);
    }

    public static boolean validate(String jwtToken) {

        return validate(jwtToken, JWT_SECRET);
    }

    public static boolean validateMarketplaceToken(String jwtToken, final String secret, final String subjectString) {

        if (jwtToken.contains(" ") || jwtToken.contains("failed ")) {

            logger.error("Marketplace token is not in token format: {}", jwtToken);
            return false;
        }
        boolean valid = false;

        // Check if string format is correct
        if ((jwtToken == null) || (!jwtToken.contains("."))) {
            return false;
        }

        int i = jwtToken.lastIndexOf('.');
        final Claims untrustedClaims = Jwts.parser().parseClaimsJwt(jwtToken.substring(0, i + 1)).getBody();

        final String subject = untrustedClaims.getSubject();
        final Date expDate = untrustedClaims.getExpiration();

        logger.debug("Marketplace token valid until {}", expDate.toString());

        if (getCurrentDate().before(expDate) && subject.equals(subjectString)) {
            valid = true;
            // logger.debug("JWT Token valid: offeringId {}; expireDate {}", offeringId.toString(), expDate);
        }

        return valid;
    }

    public static String getOfferingAccessTokenInfo(String jwtToken) {

        // Check if token string format is valid
        if ((jwtToken == null) || !jwtToken.contains(".") || jwtToken.contains(" ") || jwtToken.contains("failed ")) {
            logger.error("Marketplace token is not in valid token format: {}", jwtToken);
            return null;
        }

        int i = jwtToken.lastIndexOf('.');
        final Claims untrustedClaims = Jwts.parser().parseClaimsJwt(jwtToken.substring(0, i + 1)).getBody();

        return (String) untrustedClaims.get("subscriberId");

    }

    public static String getSubscriptionId(String jwtToken) {

        // Check if token string format is valid
        if ((jwtToken == null) || !jwtToken.contains(".") || jwtToken.contains(" ") || jwtToken.contains("failed ")) {
            logger.error("Marketplace token is not in valid token format: {}", jwtToken);
            return null;
        }

        int i = jwtToken.lastIndexOf('.');
        final Claims untrustedClaims = Jwts.parser().parseClaimsJwt(jwtToken.substring(0, i + 1)).getBody();

        return (String) untrustedClaims.getSubject(); // "subscriptionId"

    }

    public static boolean validateOfferingAccessToken(String jwtToken, final String secret, final String offeringId) {

        boolean valid = false;

        // final byte[] secretBase64Decoded = Base64.getDecoder().decode(secret);
        // final byte[] secretBase64Decoded = Base64.getUrlDecoder().decode(secret); --> This WORKED in v0.9.2
        final byte[] secretBase64Decoded = Base64.decodeBase64(secret.getBytes());

        // Check if token string format is valid
        if ((jwtToken == null) || !jwtToken.contains(".") || jwtToken.contains(" ") || jwtToken.contains("failed ")) {
            logger.error("Marketplace token is not in valid token format: {}", jwtToken);
            return false;
        }

        Claims claims;
        try {
            // This line will throw an exception if it is not a signed JWS (as expected)
            claims = Jwts.parser().setSigningKey(secretBase64Decoded).parseClaimsJws(jwtToken).getBody();
        } catch (SignatureException | MissingClaimException | IncorrectClaimException e) {
            // we get here if the required claim is not present
            logger.warn("==> WARNING: Invalid JSON Web Token: {}", e);
            return valid; // = false
        }

        final String subscribableId = (String) claims.get("subscribableId");
        // final String subject = claims.getSubject();
        final Date expDate = claims.getExpiration();

        if (getCurrentDate().before(expDate) && subscribableId.equals(offeringId)) {
            valid = true;
            // logger.debug("JWT Token valid: offeringId {}; expireDate {}", offeringId.toString(), expDate);
        } else {
            // logger.info("JWT Token is not valid or has expired!");
        }

        return valid;
    }

    public static long getExpirationTime(String jwtToken) {
        // Check if string format is correct
        if ((!jwtToken.contains("."))) {
            return 0;
        }

        int i = jwtToken.lastIndexOf('.');
        final Claims untrustedClaims = Jwts.parser().parseClaimsJwt(jwtToken.substring(0, i + 1)).getBody();

        Date expDate = untrustedClaims.getExpiration();
        return expDate.getTime();
    }

    public static boolean validate(String jwtToken, final String secret) {

        // final byte[] secretBase64Encoded = Base64.getEncoder().encode(secret.getBytes());
        final String secretBase64Encoded = Base64Variants.getDefaultVariant().encode(secret.getBytes());

        boolean valid = false;

        // Check if string format is correct
        if ((jwtToken == null) || (!jwtToken.contains("."))) {
            return false;
        }

        // Extract subject
        int i = jwtToken.lastIndexOf('.');
        final Claims untrustedClaims = Jwts.parser().parseClaimsJwt(jwtToken.substring(0, i + 1)).getBody();

        // Check if Offering ID matches a valid Offering
        // TODO
        // logger.info("Access Token for OfferingId {}", offeringId);

        // This line will throw an exception if it is not a signed JWS (as expected)
        final Claims claims = Jwts.parser().setSigningKey(secretBase64Encoded) // TODO Use appropriate key based on
                                                                               // OfferingID
                // .setSigningKeyResolver()
                .parseClaimsJws(jwtToken).getBody();

        // logger.debug("Access Token Body {}", claims.toString());

        // Object offeringId = Jwts.parser().setSigningKey(secret).parseClaimsJws(jwtToken).getBody().get("offeringId");
        // Object expireTime = Jwts.parser().setSigningKey(secret).parseClaimsJws(jwtToken).getBody().get("expireTime");

        final Date expDate = claims.getExpiration();

        if (getCurrentDate().before(expDate)) {
            valid = true;
            // logger.debug("JWT Token valid: offeringId {}; expireDate {}", offeringId.toString(), expDate);
        }

        return valid;
    }

    public static Date getCurrentDate() {
        return new Date(getCurrentTime());
    }

    public static Date getExpirationDate(long ttl) {
        return new Date(getCurrentTime() + ttl);
    }

    public static long getCurrentTime() {
        // return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return System.currentTimeMillis();
    }

    public static X509Certificate loadPublicX509(String fileName) throws GeneralSecurityException {
        InputStream is = null;
        X509Certificate crt = null;
        try {
            is = fileName.getClass().getResourceAsStream("/" + fileName);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            crt = (X509Certificate) cf.generateCertificate(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ign) {
                }
            }
        }
        return crt;
    }

}
