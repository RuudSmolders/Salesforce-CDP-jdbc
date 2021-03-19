package com.salesforce.cdp.queryservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections4.MapUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@Slf4j
public class HttpHelper {

    public static void handleErrorResponse(Response response, String propertyName) throws IOException, SQLException {
        ObjectNode node = new ObjectMapper().readValue(response.body().string(), ObjectNode.class);
        JsonNode jsonNode = node.get(propertyName);
        String message = jsonNode == null ? String.format("Property %s is not defined", propertyName) : node.get(propertyName).asText();
        throw new SQLException(message);
    }

    public static <T> T handleSuccessResponse(Response response, Class<T> type, boolean cacheResponse) throws IOException {
        String responseString = response.body().string();
        if (response.headers().get("from-local-cache") == null && cacheResponse) {
            log.info("Caching the response");
            MetadataCacheUtil.cacheMetadata(response.request().url().toString(), responseString);
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(responseString, type);
    }

    protected static Request buildRequest(String method, String url, RequestBody body, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .method(method, body == null ? null : body);
        if (!MapUtils.isEmpty(headers)) {
            for (String key : headers.keySet()) {
                builder.addHeader(key, headers.get(key));
            }
        }
        if (!headers.containsKey(Constants.USER_AGENT)) {
            builder.addHeader(Constants.USER_AGENT, Constants.USER_AGENT_VALUE);
        }
        return builder.build();
    }
}