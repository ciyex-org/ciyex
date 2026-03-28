package org.ciyex.ehr.fhir;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * HAPI FHIR client interceptor that forwards the current user's JWT
 * as a Bearer token on all outgoing FHIR server requests.
 *
 * <p>Reads the token from Spring Security's {@link SecurityContextHolder}
 * on each request, so it works correctly with cached/shared
 * {@link ca.uhn.fhir.rest.client.api.IGenericClient} instances.</p>
 */
@Component
@Slf4j
public class FhirBearerTokenInterceptor implements IClientInterceptor {

    @Override
    public void interceptRequest(IHttpRequest theRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String tokenValue = jwtAuth.getToken().getTokenValue();
            theRequest.addHeader("Authorization", "Bearer " + tokenValue);
            log.trace("Forwarded user JWT to FHIR server request");
        }
    }

    @Override
    public void interceptResponse(IHttpResponse theResponse) {
        // no-op
    }
}
