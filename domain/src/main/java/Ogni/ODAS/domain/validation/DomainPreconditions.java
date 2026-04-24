package Ogni.ODAS.domain.validation;

import Ogni.ODAS.domain.exception.DomainValidationException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public final class DomainPreconditions {

    private DomainPreconditions() {
    }

    public static <T> T notNull(T value, String fieldName) {
        if (value == null) {
            throw new DomainValidationException(fieldName + " must not be null");
        }
        return value;
    }

    public static String notBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(fieldName + " must not be blank");
        }
        return value;
    }

    public static String maxLength(String value, int maxLength, String fieldName) {
        notNull(value, fieldName);
        if (value.length() > maxLength) {
            throw new DomainValidationException(fieldName + " must not be longer than " + maxLength + " characters");
        }
        return value;
    }

    public static <T extends Collection<?>> T notEmpty(T value, String fieldName) {
        notNull(value, fieldName);
        if (value.isEmpty()) {
            throw new DomainValidationException(fieldName + " must not be empty");
        }
        return value;
    }

    public static Integer nonNegative(Integer value, String fieldName) {
        notNull(value, fieldName);
        if (value < 0) {
            throw new DomainValidationException(fieldName + " must not be negative");
        }
        return value;
    }

    public static Long positive(Long value, String fieldName) {
        notNull(value, fieldName);
        if (value <= 0) {
            throw new DomainValidationException(fieldName + " must be positive");
        }
        return value;
    }

    public static Integer inRange(Integer value, int minInclusive, int maxInclusive, String fieldName) {
        notNull(value, fieldName);
        if (value < minInclusive || value > maxInclusive) {
            throw new DomainValidationException(
                    fieldName + " must be between " + minInclusive + " and " + maxInclusive
            );
        }
        return value;
    }

    public static Long validateId(Long id, String fieldName) {
        if (id != null) {
            DomainPreconditions.positive(id, fieldName);
        }
        return id;
    }

    public static List<Long> normalizeNonEmptyIds(Collection<Long> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw new DomainValidationException(fieldName + " must contain at least one id");
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null) {
                normalized.add(value);
            }
        }
        if (normalized.isEmpty()) {
            throw new DomainValidationException(fieldName + " must contain at least one id");
        }
        return List.copyOf(normalized);
    }

    public static List<Long> normalizeOptionalIds(Collection<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    public static void require(boolean expression, String message) {
        if (!expression) {
            throw new DomainValidationException(message);
        }
    }
}
