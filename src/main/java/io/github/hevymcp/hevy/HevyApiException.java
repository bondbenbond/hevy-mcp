package io.github.hevymcp.hevy;

public final class HevyApiException extends RuntimeException {

    private final HevyErrorCode code;

    public HevyApiException(HevyErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public HevyApiException(HevyErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public HevyErrorCode code() {
        return code;
    }
}
