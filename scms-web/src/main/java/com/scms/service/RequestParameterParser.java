package com.scms.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RequestParameterParser {

    public <T> T getListValue(java.util.List<T> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    public Double parseDoubleOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public double parseDoubleOrDefault(String value, double defaultValue) {
        Double parsed = parseDoubleOrNull(value);
        return parsed != null ? parsed : defaultValue;
    }

    public Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public Map<Long, Integer> parsePositiveIntegerMap(Map<String, String> params, String keyPrefix) {
        Map<Long, Integer> result = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!entry.getKey().startsWith(keyPrefix)) {
                continue;
            }

            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }

            try {
                Long id = Long.parseLong(entry.getKey().substring(keyPrefix.length()));
                int quantity = Integer.parseInt(value.trim());
                if (quantity > 0) {
                    result.put(id, quantity);
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed form fields and keep processing the rest.
            }
        }

        return result;
    }

    public Map<Long, Double> parseSelectedPositiveDoubleMap(Map<String, String> params,
                                                            String quantityPrefix,
                                                            String selectionPrefix) {
        Map<Long, Double> result = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!entry.getKey().startsWith(quantityPrefix)) {
                continue;
            }

            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }

            try {
                Long id = Long.parseLong(entry.getKey().substring(quantityPrefix.length()));
                if (!params.containsKey(selectionPrefix + id)) {
                    continue;
                }

                double quantity = Double.parseDouble(value.trim());
                if (quantity > 0) {
                    result.put(id, quantity);
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed form fields and keep processing the rest.
            }
        }

        return result;
    }
}