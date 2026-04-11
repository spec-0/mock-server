package io.spec0.mockserver.mockgen;

import java.util.HashMap;
import java.util.Map;

public class MockResponseStore {
    private final Map<String, Map<String, Object>> mockResponses = new HashMap<>();

    public void storeMockResponse(String operationId, String responseCode, Object mockData) {
        mockResponses.computeIfAbsent(operationId, k -> new HashMap<>()).put(responseCode, mockData);
    }

    public Object getMockResponse(String operationId, String responseCode) {
        return mockResponses.getOrDefault(operationId, new HashMap<>()).get(responseCode);
    }

    public Map<String, Map<String, Object>> getAllMockResponses() {
        return mockResponses;
    }
}
