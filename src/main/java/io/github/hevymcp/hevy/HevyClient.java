package io.github.hevymcp.hevy;

import io.github.hevymcp.config.HevyProperties;
import io.github.hevymcp.hevy.model.Routine;
import io.github.hevymcp.hevy.model.RoutinePage;
import io.github.hevymcp.hevy.model.RoutineResponse;
import io.github.hevymcp.hevy.model.RoutineUpdateRequest;
import io.github.hevymcp.hevy.model.Workout;
import io.github.hevymcp.hevy.model.WorkoutPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public final class HevyClient {

    private static final Logger log = LoggerFactory.getLogger(HevyClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_PAGE_SIZE = 10;

    private final WebClient webClient;

    public HevyClient(WebClient.Builder builder, HevyProperties properties) {
        this.webClient = builder
                .baseUrl(properties.baseUrl().toString())
                .defaultHeader("api-key", properties.apiKey())
                .build();
    }

    public WorkoutPage getWorkouts(int page, int pageSize) {
        validatePage(page, pageSize);
        log.info("Calling Hevy operation=get_workouts page={} pageSize={}", page, pageSize);
        return execute(webClient.get().uri(uri -> uri.path("/v1/workouts")
                .queryParam("page", page).queryParam("pageSize", pageSize).build())
                .retrieve().onStatus(HttpStatusCode::isError, response -> upstreamError(response.statusCode(), "workouts"))
                .bodyToMono(WorkoutPage.class), "workouts");
    }

    public Workout getWorkout(String workoutId) {
        requireId(workoutId, "workout");
        log.info("Calling Hevy operation=get_workout workoutId={}", workoutId);
        return execute(webClient.get().uri("/v1/workouts/{id}", workoutId)
                .retrieve().onStatus(HttpStatusCode::isError, response -> upstreamError(response.statusCode(), "workout"))
                .bodyToMono(Workout.class), "workout");
    }

    public RoutinePage getRoutines(int page, int pageSize) {
        validatePage(page, pageSize);
        log.info("Calling Hevy operation=get_routines page={} pageSize={}", page, pageSize);
        return execute(webClient.get().uri(uri -> uri.path("/v1/routines")
                .queryParam("page", page).queryParam("pageSize", pageSize).build())
                .retrieve().onStatus(HttpStatusCode::isError, response -> upstreamError(response.statusCode(), "routines"))
                .bodyToMono(RoutinePage.class), "routines");
    }

    public Routine getRoutine(String routineId) {
        requireId(routineId, "routine");
        log.info("Calling Hevy operation=get_routine routineId={}", routineId);
        RoutineResponse response = execute(webClient.get().uri("/v1/routines/{id}", routineId)
                .retrieve().onStatus(HttpStatusCode::isError, upstream -> upstreamError(upstream.statusCode(), "routine"))
                .bodyToMono(RoutineResponse.class), "routine");
        if (response.routine() == null) {
            throw new HevyApiException(HevyErrorCode.INVALID_RESPONSE, "The Hevy API returned an invalid routine response.");
        }
        return response.routine();
    }

    public Routine updateRoutine(String routineId, RoutineUpdateRequest request) {
        requireId(routineId, "routine");
        if (request == null || request.routine() == null) {
            throw new HevyApiException(HevyErrorCode.BAD_REQUEST, "A routine update body is required.");
        }
        log.info("Calling Hevy operation=update_routine routineId={}", routineId);
        return execute(webClient.put().uri("/v1/routines/{id}", routineId).bodyValue(request)
                .retrieve().onStatus(HttpStatusCode::isError, upstream -> upstreamError(upstream.statusCode(), "routine"))
                .bodyToMono(Routine.class), "routine");
    }

    private <T> T execute(Mono<T> response, String resource) {
        try {
            T value = response.timeout(REQUEST_TIMEOUT).block();
            if (value == null) {
                throw new HevyApiException(HevyErrorCode.INVALID_RESPONSE, "The Hevy API returned an empty response.");
            }
            return value;
        }
        catch (HevyApiException ex) {
            throw ex;
        }
        catch (WebClientRequestException ex) {
            log.warn("Hevy operation failed resource={} reason=network_or_timeout", resource);
            throw new HevyApiException(HevyErrorCode.UNAVAILABLE, "The Hevy API is temporarily unavailable.", ex);
        }
        catch (DecodingException ex) {
            log.warn("Hevy operation failed resource={} reason=invalid_response", resource);
            throw new HevyApiException(HevyErrorCode.INVALID_RESPONSE, "The Hevy API returned an invalid response.", ex);
        }
        catch (RuntimeException ex) {
            if (ex.getCause() instanceof java.util.concurrent.TimeoutException) {
                throw new HevyApiException(HevyErrorCode.UNAVAILABLE, "The Hevy API request timed out.", ex);
            }
            throw ex;
        }
    }

    private Mono<? extends Throwable> upstreamError(HttpStatusCode status, String resource) {
        log.warn("Hevy upstream failure resource={} status={}", resource, status.value());
        String noun = "routine".equals(resource) ? "routine" : "workout";
        return Mono.error(switch (status.value()) {
            case 400 -> new HevyApiException(HevyErrorCode.BAD_REQUEST, "Hevy rejected the request as invalid.");
            case 401 -> new HevyApiException(HevyErrorCode.CREDENTIALS_REJECTED,
                    "The Hevy API rejected the configured API credentials.");
            case 403 -> new HevyApiException(HevyErrorCode.FORBIDDEN,
                    "The Hevy API denied this operation.");
            case 404 -> new HevyApiException(HevyErrorCode.NOT_FOUND,
                    "The requested Hevy " + noun + " was not found.");
            case 429 -> new HevyApiException(HevyErrorCode.RATE_LIMITED,
                    "The Hevy API rate limit was reached. Try again later.");
            default -> new HevyApiException(HevyErrorCode.UNAVAILABLE,
                    "The Hevy API is temporarily unavailable.");
        });
    }

    private static void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new HevyApiException(HevyErrorCode.BAD_REQUEST,
                    "Page must be at least 1 and page size must be between 1 and 10.");
        }
    }

    private static void requireId(String id, String resource) {
        if (id == null || id.isBlank()) {
            throw new HevyApiException(HevyErrorCode.BAD_REQUEST, "A " + resource + " ID is required.");
        }
    }
}
