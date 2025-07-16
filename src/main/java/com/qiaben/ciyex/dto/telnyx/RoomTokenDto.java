package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class RoomTokenDto {

    @Data
    public static class GenerateTokenRequest {
        private Integer token_ttl_secs;
        private Integer refresh_token_ttl_secs;
    }

    @Data
    public static class RefreshTokenRequest {
        private Integer token_ttl_secs;
        private String refresh_token;
    }

    @Data
    public static class RoomTokenResponse {
        private DataBody data;

        @Data
        public static class DataBody {
            private String token;
            private String token_expires_at;
            private String refresh_token;
            private String refresh_token_expires_at;
        }
    }
}
