package Ogni.ODAS.boot.temp;

import java.time.OffsetDateTime;

public record TemporaryErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
