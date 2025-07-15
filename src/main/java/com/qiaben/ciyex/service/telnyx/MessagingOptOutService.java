package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.MessagingOptOutQueryParams;
import com.qiaben.ciyex.dto.telnyx.MessagingOptOutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class MessagingOptOutService {

    private final TelnyxProperties props;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
    }

    public MessagingOptOutResponse listOptOuts(MessagingOptOutQueryParams params) {
        return client()
                .get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/messaging/opt_outs");
                    if (params.getMessagingProfileId() != null)
                        builder.queryParam("filter[messaging_profile_id]", params.getMessagingProfileId());
                    if (params.getFrom() != null)
                        builder.queryParam("filter[from]", params.getFrom());
                    if (params.getCreatedAfter() != null)
                        builder.queryParam("created_at[gte]", params.getCreatedAfter());
                    if (params.getCreatedBefore() != null)
                        builder.queryParam("created_at[lte]", params.getCreatedBefore());
                    if (params.getRedactionEnabled() != null)
                        builder.queryParam("redaction_enabled", params.getRedactionEnabled());
                    builder.queryParam("page[number]", params.getPageNumber() != null ? params.getPageNumber() : 1);
                    builder.queryParam("page[size]", params.getPageSize() != null ? params.getPageSize() : 20);
                    return builder.build();
                })
                .retrieve()
                .body(MessagingOptOutResponse.class);
    }
}
