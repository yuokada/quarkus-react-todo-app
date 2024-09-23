package io.github.yuokada.practice;

@Deprecated
public record Users(
    String key,
    long value
) {

    public Users {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key cannot be null or blank");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Value cannot be negative");
        }
    }
}
