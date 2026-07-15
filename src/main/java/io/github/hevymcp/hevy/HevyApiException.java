package io.github.hevymcp.hevy;

public final class HevyApiException extends RuntimeException {

    private final HevyErrorCode code;
    private final Integer httpStatus;

    public HevyApiException(HevyErrorCode code, String message) {
        this(code, message, null, null);
    }

    public HevyApiException(HevyErrorCode code, String message, Throwable cause) {
        this(code, message, cause, null);
    }

    public HevyApiException(HevyErrorCode code, String message, Throwable cause, Integer httpStatus) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public HevyErrorCode code() {
        return code;
    }

    public Integer httpStatus() {
        return httpStatus;
    }
}
