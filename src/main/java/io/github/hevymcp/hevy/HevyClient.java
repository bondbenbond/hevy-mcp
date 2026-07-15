package io.github.hevymcp.hevy;

import io.github.hevymcp.config.HevyProperties;
import io.github.hevymcp.hevy.model.ExerciseTemplate;
import io.github.hevymcp.hevy.model.ExerciseTemplatePage;
import io.github.hevymcp.hevy.model.ExerciseTemplateSearchResult;
import io.github.hevymcp.hevy.model.ExerciseHistory;
import io.github.hevymcp.hevy.model.Routine;
import io.github.hevymcp.hevy.model.RoutinePage;
import io.github.hevymcp.hevy.model.RoutineResponse;
import io.github.hevymcp.hevy.model.HevyUpdateRoutineRequest;
import io.github.hevymcp.hevy.model.RoutineFolder;
import io.github.hevymcp.hevy.model.RoutineFolderPage;
import io.github.hevymcp.hevy.model.Workout;
import io.github.hevymcp.hevy.model.WorkoutPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public final class HevyClient {

    private static final Logger log = LoggerFactory.getLogger(HevyClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_WORKOUT_ROUTINE_PAGE_SIZE = 10;
    private static final int MAX_EXERCISE_TEMPLATE_PAGE_SIZE = 100;
    private static final int MAX_EXERCISE_TEMPLATE_SEARCH_RESULTS = 100;

    private final WebClient webClient;

    public HevyClient(WebClient.Builder builder, HevyProperties properties) {
        this.webClient = builder
                .baseUrl(properties.baseUrl().toString())
                .defaultHeader("api-key", properties.apiKey())
                .build();
    }

    public WorkoutPage getWorkouts(int page, int pageSize) {
        validatePage(page, pageSize, MAX_WORKOUT_ROUTINE_PAGE_SIZE);
        log.info("Calling Hevy operation=get_workouts page={} pageSize={}", page, pageSize);
        return execute(webClient.get().uri(uri -> uri.path("/v1/workouts")
                .queryParam("page", page).queryParam("pageSize", pageSize).build())
                .retrieve().onStatus(HttpStatusCode::isError, response -> upstreamError(response, "workouts"))
                .bodyToMono(WorkoutPage.class), "workouts");
    }

    public Workout getWorkout(String workoutId) {
        requireId(workoutId, "workout");
        log.info("Calling Hevy operation=get_workout workoutId={}", workoutId);
        return execute(webClient.get().uri("/v1/workouts/{id}", workoutId)
                .retrieve().onStatus(HttpStatusCode::isError, response -> upstreamError(response, "workout"))
                .bodyToMono(Workout.class), "workout");
    }

    public RoutinePage getRoutines(int page, int pageSize) {
        validatePage(page, pageSize, MAX_WORKOUT_ROUTINE_PAGE_SIZE);
        log.info("Calling Hevy operation=get_routines page={} pageSize={}", page, pageSize);
        return execute(webClient.get().uri(uri -> uri.path("/v1/routines")
                .queryParam("page", page).queryParam("pageSize", pageSize).build())
                .retrieve().onStatus(HttpStatusCode::isError, response -> upstreamError(response, "routines"))
                .bodyToMono(RoutinePage.class), "routines");
    }

    public Routine getRoutine(String routineId) {
        requireId(routineId, "routine");
        log.info("Calling Hevy operation=get_routine routineId={}", routineId);
        RoutineResponse response = execute(webClient.get().uri("/v1/routines/{id}", routineId)
                .retrieve().onStatus(HttpStatusCode::isError, upstream -> upstreamError(upstream, "routine"))
                .bodyToMono(RoutineResponse.class), "routine");
        if (response.routine() == null) {
            throw new HevyApiException(HevyErrorCode.INVALID_RESPONSE, "The Hevy API returned an invalid routine response.");
        }
        return response.routine();
    }

    public void putRoutine(String routineId, HevyUpdateRoutineRequest request) {
        requireId(routineId, "routine");
        if (request == null || request.routine() == null) {
            throw new HevyApiException(HevyErrorCode.BAD_REQUEST, "A routine update body is required.");
        }
        log.info("Calling Hevy operation=update_routine routineId={}", routineId);
        execute(webClient.put().uri("/v1/routines/{id}", routineId).bodyValue(request)
                .retrieve().onStatus(HttpStatusCode::isError, upstream -> upstreamError(upstream, "routine"))
                .toBodilessEntity(), "routine update");
    }

    public ExerciseTemplatePage getExerciseTemplates(int page, int pageSize) {
        validatePage(page, pageSize, MAX_EXERCISE_TEMPLATE_PAGE_SIZE);
        log.info("Calling Hevy operation=get_exercise_templates page={} pageSize={}", page, pageSize);
        return execute(webClient.get().uri(uri -> uri.path("/v1/exercise_templates")
                .queryParam("page", page).queryParam("pageSize", pageSize).build())
                .retrieve().onStatus(HttpStatusCode::isError,
                        response -> upstreamError(response, "exercise templates"))
                .bodyToMono(ExerciseTemplatePage.class), "exercise templates");
    }

    public ExerciseTemplate getExerciseTemplate(String exerciseTemplateId) {
        requireId(exerciseTemplateId, "exercise template");
        log.info("Calling Hevy operation=get_exercise_template exerciseTemplateId={}", exerciseTemplateId);
        return execute(webClient.get().uri("/v1/exercise_templates/{id}", exerciseTemplateId)
                .retrieve().onStatus(HttpStatusCode::isError,
                        response -> upstreamError(response, "exercise template"))
                .bodyToMono(ExerciseTemplate.class), "exercise template");
    }

    public ExerciseTemplateSearchResult searchExerciseTemplates(String query, int maxResults) {
        if (query == null || query.isBlank()) {
            throw new HevyApiException(HevyErrorCode.BAD_REQUEST, "An exercise template search query is required.");
        }
        if (maxResults < 1 || maxResults > MAX_EXERCISE_TEMPLATE_SEARCH_RESULTS) {
            throw new HevyApiException(HevyErrorCode.BAD_REQUEST,
                    "Maximum search results must be between 1 and 100.");
        }

        String normalizedQuery = query.strip().toLowerCase(Locale.ROOT);
        var matches = new ArrayList<ExerciseTemplate>();
        int page = 1;
        int pageCount;
        do {
            ExerciseTemplatePage result = getExerciseTemplates(page, MAX_EXERCISE_TEMPLATE_PAGE_SIZE);
            if (result.exerciseTemplates() != null) {
                result.exerciseTemplates().stream()
                        .filter(template -> matches(template, normalizedQuery))
                        .limit(maxResults - matches.size())
                        .forEach(matches::add);
            }
            pageCount = result.pageCount() == null ? page : result.pageCount();
            page++;
        }
        while (page <= pageCount && matches.size() < maxResults);

        return new ExerciseTemplateSearchResult(query.strip(), matches.size(), List.copyOf(matches));
    }

    public ExerciseHistory getExerciseHistory(String exerciseTemplateId, String startDate, String endDate) {
        requireId(exerciseTemplateId, "exercise template");
        log.info("Calling Hevy operation=get_exercise_history exerciseTemplateId={}", exerciseTemplateId);
        return execute(webClient.get().uri(uri -> {
                    var builder = uri.path("/v1/exercise_history/{id}");
                    if (startDate != null && !startDate.isBlank()) {
                        builder.queryParam("start_date", startDate);
                    }
                    if (endDate != null && !endDate.isBlank()) {
                        builder.queryParam("end_date", endDate);
                    }
                    return builder.build(exerciseTemplateId);
                })
                .retrieve().onStatus(HttpStatusCode::isError,
                        response -> upstreamError(response, "exercise history"))
                .bodyToMono(ExerciseHistory.class), "exercise history");
    }

    public RoutineFolderPage getRoutineFolders(int page, int pageSize) {
        validatePage(page, pageSize, MAX_WORKOUT_ROUTINE_PAGE_SIZE);
        log.info("Calling Hevy operation=get_routine_folders page={} pageSize={}", page, pageSize);
        return execute(webClient.get().uri(uri -> uri.path("/v1/routine_folders")
                .queryParam("page", page).queryParam("pageSize", pageSize).build())
                .retrieve().onStatus(HttpStatusCode::isError,
                        response -> upstreamError(response, "routine folders"))
                .bodyToMono(RoutineFolderPage.class), "routine folders");
    }

    public RoutineFolder getRoutineFolder(String folderId) {
        requireId(folderId, "routine folder");
        log.info("Calling Hevy operation=get_routine_folder folderId={}", folderId);
        return execute(webClient.get().uri("/v1/routine_folders/{id}", folderId)
                .retrieve().onStatus(HttpStatusCode::isError,
                        response -> upstreamError(response, "routine folder"))
                .bodyToMono(RoutineFolder.class), "routine folder");
    }

    private static boolean matches(ExerciseTemplate template, String query) {
        if (contains(template.id(), query)
                || contains(template.title(), query)
                || contains(template.type(), query)
                || contains(template.primaryMuscleGroup(), query)
                || contains(template.equipmentCategory(), query)) {
            return true;
        }
        return template.secondaryMuscleGroups() != null
                && template.secondaryMuscleGroups().stream().anyMatch(value -> contains(value, query));
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
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

    private Mono<? extends Throwable> upstreamError(ClientResponse response, String resource) {
        HttpStatusCode status = response.statusCode();
        log.warn("Hevy upstream failure resource={} status={}", resource, status.value());
        return response.bodyToMono(UpstreamErrorBody.class)
                .defaultIfEmpty(new UpstreamErrorBody(null, null))
                .onErrorReturn(new UpstreamErrorBody(null, null))
                .map(body -> upstreamException(status, resource, safeValidationDetail(body)));
    }

    private HevyApiException upstreamException(HttpStatusCode status, String resource, String validationDetail) {
        String noun = switch (resource) {
            case "routine", "routines" -> "routine";
            case "exercise template", "exercise templates" -> "exercise template";
            case "routine folder", "routine folders" -> "routine folder";
            case "exercise history" -> "exercise history";
            default -> "workout";
        };
        return switch (status.value()) {
            case 400 -> withStatus(HevyErrorCode.BAD_REQUEST,
                    "Hevy rejected the request as invalid." + validationDetail, status);
            case 401 -> withStatus(HevyErrorCode.CREDENTIALS_REJECTED,
                    "The Hevy API rejected the configured API credentials.", status);
            case 403 -> withStatus(HevyErrorCode.FORBIDDEN,
                    "The Hevy API denied this operation.", status);
            case 404 -> withStatus(HevyErrorCode.NOT_FOUND,
                    "The requested Hevy " + noun + " was not found.", status);
            case 429 -> withStatus(HevyErrorCode.RATE_LIMITED,
                    "The Hevy API rate limit was reached. Try again later.", status);
            default -> withStatus(HevyErrorCode.UNAVAILABLE,
                    "The Hevy API is temporarily unavailable.", status);
        };
    }

    private static HevyApiException withStatus(HevyErrorCode code, String message, HttpStatusCode status) {
        return new HevyApiException(code, message, null, status.value());
    }

    private static String safeValidationDetail(UpstreamErrorBody body) {
        String detail = body.error() == null || body.error().isBlank() ? body.message() : body.error();
        if (detail == null || detail.isBlank()) {
            return "";
        }
        String singleLine = detail.replaceAll("[\\p{Cntrl}]+", " ").strip();
        if (singleLine.length() > 300) {
            singleLine = singleLine.substring(0, 300) + "…";
        }
        return " Hevy reported: " + singleLine;
    }

    private static void validatePage(int page, int pageSize, int maxPageSize) {
        if (page < 1 || pageSize < 1 || pageSize > maxPageSize) {
            throw new HevyApiException(HevyErrorCode.BAD_REQUEST,
                    "Page must be at least 1 and page size must be between 1 and " + maxPageSize + ".");
        }
    }

    private static void requireId(String id, String resource) {
        if (id == null || id.isBlank()) {
            throw new HevyApiException(HevyErrorCode.BAD_REQUEST, "A " + resource + " ID is required.");
        }
    }

    private record UpstreamErrorBody(String error, String message) {
    }
}
