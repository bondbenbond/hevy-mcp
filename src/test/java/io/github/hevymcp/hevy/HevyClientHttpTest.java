package io.github.hevymcp.hevy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.hevymcp.config.HevyProperties;
import io.github.hevymcp.hevy.model.HevyUpdateRoutineRequest;
import io.github.hevymcp.mcp.model.RoutineUpdateInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HevyClientHttpTest {

    private HttpServer server;
    private HevyClient client;
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> responseBody = new AtomicReference<>("{}");
    private final AtomicReference<Request> request = new AtomicReference<>();
    private final Queue<Response> responses = new ConcurrentLinkedQueue<>();
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();

        JsonMapper mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        ExchangeStrategies strategies = ExchangeStrategies.builder().codecs(codecs -> {
            codecs.defaultCodecs().jacksonJsonDecoder(new JacksonJsonDecoder(mapper));
            codecs.defaultCodecs().jacksonJsonEncoder(new JacksonJsonEncoder(mapper));
        }).build();
        WebClient.Builder builder = WebClient.builder().exchangeStrategies(strategies);
        client = new HevyClient(builder, new HevyProperties(
                "fake-api-key", URI.create("http://127.0.0.1:" + server.getAddress().getPort())));
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getsAndDeserializesWorkoutPageWithPaginationAndApiKey() {
        responseBody.set("""
                {"page":2,"page_count":3,"workouts":[{"id":"w1","title":"Training",
                "routine_id":"r1","start_time":"2026-01-01T10:00:00Z","exercises":[{
                "index":0,"title":"Squat","exercise_template_id":"squat","sets":[{
                "index":0,"type":"normal","weight_kg":100.5,"reps":5}]}]}]}
                """);

        var result = client.getWorkouts(2, 10);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.pageCount()).isEqualTo(3);
        assertThat(result.workouts().getFirst().routineId()).isEqualTo("r1");
        assertThat(result.workouts().getFirst().exercises().getFirst().sets().getFirst().weightKg())
                .isEqualByComparingTo("100.5");
        assertThat(request.get().path()).isEqualTo("/v1/workouts?page=2&pageSize=10");
        assertThat(request.get().apiKey()).isEqualTo("fake-api-key");
    }

    @Test
    void getsAndDeserializesWorkoutById() {
        responseBody.set("""
                {"id":"workout-id","title":"Training","exercises":[]}
                """);

        assertThat(client.getWorkout("workout-id").id()).isEqualTo("workout-id");
        assertThat(request.get().path()).isEqualTo("/v1/workouts/workout-id");
    }

    @Test
    void getsAndDeserializesRoutinePage() {
        responseBody.set("""
                {"page":1,"page_count":1,"routines":[{"id":"r1","title":"Upper",
                "folder_id":42,"exercises":[]}]}
                """);

        var result = client.getRoutines(1, 5);

        assertThat(result.routines().getFirst().folderId()).isEqualTo(42);
        assertThat(request.get().path()).isEqualTo("/v1/routines?page=1&pageSize=5");
    }

    @Test
    void getsWrappedRoutineById() {
        responseBody.set("""
                {"routine":{"id":"r1","title":"Upper","exercises":[]}}
                """);

        assertThat(client.getRoutine("r1").title()).isEqualTo("Upper");
        assertThat(request.get().path()).isEqualTo("/v1/routines/r1");
    }

    @Test
    void getsAndDeserializesExerciseTemplatePage() {
        responseBody.set("""
                {"page":1,"page_count":1,"exercise_templates":[{"id":"template-1",
                "title":"Incline Bench Press","type":"weight_reps","primary_muscle_group":"chest",
                "secondary_muscle_groups":["triceps"],"equipment_category":"dumbbell","is_custom":false}]}
                """);

        var result = client.getExerciseTemplates(1, 100);

        assertThat(result.exerciseTemplates().getFirst().primaryMuscleGroup()).isEqualTo("chest");
        assertThat(result.exerciseTemplates().getFirst().equipmentCategory()).isEqualTo("dumbbell");
        assertThat(request.get().path()).isEqualTo("/v1/exercise_templates?page=1&pageSize=100");
    }

    @Test
    void getsExerciseTemplateById() {
        responseBody.set("""
                {"id":"template-1","title":"Incline Bench Press","type":"weight_reps",
                "primary_muscle_group":"chest","secondary_muscle_groups":[],
                "equipment_category":"dumbbell","is_custom":false}
                """);

        assertThat(client.getExerciseTemplate("template-1").title()).isEqualTo("Incline Bench Press");
        assertThat(request.get().path()).isEqualTo("/v1/exercise_templates/template-1");
    }

    @Test
    void searchesExerciseTemplatesAcrossRelevantFields() {
        responseBody.set("""
                {"page":1,"page_count":1,"exercise_templates":[
                {"id":"template-1","title":"Incline Bench Press","type":"weight_reps",
                "primary_muscle_group":"chest","secondary_muscle_groups":["triceps"],
                "equipment_category":"dumbbell","is_custom":false},
                {"id":"template-2","title":"Back Squat","type":"weight_reps",
                "primary_muscle_group":"quadriceps","secondary_muscle_groups":["glutes"],
                "equipment_category":"barbell","is_custom":false}]}
                """);

        var result = client.searchExerciseTemplates("DUMBBELL", 25);

        assertThat(result.query()).isEqualTo("DUMBBELL");
        assertThat(result.matchCount()).isEqualTo(1);
        assertThat(result.exerciseTemplates()).extracting("id").containsExactly("template-1");
    }

    @Test
    void rejectsInvalidExerciseTemplateSearchBeforeCallingUpstream() {
        assertThatThrownBy(() -> client.searchExerciseTemplates(" ", 25))
                .isInstanceOfSatisfying(HevyApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo(HevyErrorCode.BAD_REQUEST));
        assertThatThrownBy(() -> client.searchExerciseTemplates("bench", 101))
                .isInstanceOfSatisfying(HevyApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo(HevyErrorCode.BAD_REQUEST));
        assertThat(request.get()).isNull();
    }

    @Test
    void getsExerciseHistoryWithOptionalDateRange() {
        responseBody.set("""
                {"exercise_history":[{"workout_id":"workout-1","workout_title":"Upper",
                "workout_start_time":"2026-07-01T10:00:00Z","workout_end_time":"2026-07-01T11:00:00Z",
                "exercise_template_id":"template-1","weight_kg":80.5,"reps":8,
                "distance_meters":null,"duration_seconds":null,"rpe":8.5,
                "custom_metric":null,"set_type":"normal"}]}
                """);

        var result = client.getExerciseHistory(
                "template-1", "2026-07-01T00:00:00Z", "2026-07-31T23:59:59Z");

        assertThat(result.exerciseHistory()).hasSize(1);
        assertThat(result.exerciseHistory().getFirst().weightKg()).isEqualByComparingTo("80.5");
        assertThat(request.get().path())
                .startsWith("/v1/exercise_history/template-1?")
                .contains("start_date=2026-07-01T00:00:00Z")
                .contains("end_date=2026-07-31T23:59:59Z");
    }

    @Test
    void getsRoutineFolderPageAndFolderById() {
        responseBody.set("""
                {"page":1,"page_count":1,"routine_folders":[{"id":42,"index":0,
                "title":"Push Pull","updated_at":"2026-07-01T00:00:00Z",
                "created_at":"2026-06-01T00:00:00Z"}]}
                """);

        var page = client.getRoutineFolders(1, 10);

        assertThat(page.routineFolders().getFirst().id()).isEqualTo(42L);
        assertThat(request.get().path()).isEqualTo("/v1/routine_folders?page=1&pageSize=10");

        responseBody.set("""
                {"id":42,"index":0,"title":"Push Pull","updated_at":"2026-07-01T00:00:00Z",
                "created_at":"2026-06-01T00:00:00Z"}
                """);

        assertThat(client.getRoutineFolder("42").title()).isEqualTo("Push Pull");
        assertThat(request.get().path()).isEqualTo("/v1/routine_folders/42");
    }

    @Test
    void serializesRoutineUpdateAndIgnoresPartialResponse() {
        responseBody.set("""
                {"id":"r1","title":"Upper revised","exercises":[]}
                """);
        var set = new HevyUpdateRoutineRequest.SetUpdate(
                "normal", new BigDecimal("80.5"), 8, null, null, null, null);
        var exercise = new HevyUpdateRoutineRequest.ExerciseUpdate(
                "template-1", 7, 90, "Controlled", List.of(set));
        var update = new HevyUpdateRoutineRequest(new HevyUpdateRoutineRequest.RoutineUpdate(
                "Upper revised", "Notes", List.of(exercise)));

        client.putRoutine("r1", update);
        assertThat(request.get().method()).isEqualTo("PUT");
        assertThat(request.get().path()).isEqualTo("/v1/routines/r1");
        assertThat(request.get().body())
                .contains("\"exercise_template_id\":\"template-1\"")
                .contains("\"superset_id\":7")
                .doesNotContain("supersets_id")
                .contains("\"rest_seconds\":90")
                .contains("\"weight_kg\":80.5")
                .doesNotContain("distance_meters", "duration_seconds", "custom_metric", "rep_range");
    }

    @Test
    void updateWorkflowAccepts204ThenFetchesCanonicalRoutine() {
        responses.add(new Response(200, """
                {"routine":{"id":"r1","title":"Squat Week 1","notes":"Existing notes",
                "updated_at":"before","created_at":"created","exercises":[{"index":0,
                "title":"Squat","rest_seconds":180,"notes":"Keep this","exercise_template_id":"squat",
                "supersets_id":12,"sets":[{"index":0,"type":"normal","weight_kg":70.3,"reps":5}] }]}}
                """));
        responses.add(new Response(204, ""));
        responses.add(new Response(200, """
                {"routine":{"id":"r1","title":"Squat Week 1 revised","notes":"Existing notes",
                "updated_at":"after","created_at":"created","exercises":[{"index":0,
                "title":"Squat","rest_seconds":180,"notes":"Keep this","exercise_template_id":"squat",
                "supersets_id":12,"sets":[{"index":0,"type":"normal","weight_kg":70.3,"reps":5}] }]}}
                """));
        var input = new RoutineUpdateInput(
                new RoutineUpdateInput.RoutineUpdate("Squat Week 1 revised", "", null));
        var service = new RoutineUpdateService(client, new RoutineUpdateMapper());

        var result = service.updateRoutine("r1", input);

        assertThat(result.title()).isEqualTo("Squat Week 1 revised");
        assertThat(result.createdAt()).isEqualTo("created");
        assertThat(result.exercises()).hasSize(1);
        assertThat(requests).extracting(Request::method).containsExactly("GET", "PUT", "GET");
        assertThat(requests).extracting(Request::path).containsExactly(
                "/v1/routines/r1", "/v1/routines/r1", "/v1/routines/r1");
        assertThat(requests.get(1).body())
                .contains("\"notes\":\"Existing notes\"")
                .contains("\"exercise_template_id\":\"squat\"")
                .contains("\"superset_id\":12")
                .doesNotContain("duration_seconds", "distance_meters");
    }

    @Test
    void includesSafeHevyValidationDetailForBadRequests() {
        responseStatus.set(400);
        responseBody.set("""
                {"error":"sets[0].type must be one of warmup, normal, failure, dropset"}
                """);

        assertThatThrownBy(() -> client.getWorkout("w1"))
                .isInstanceOfSatisfying(HevyApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(HevyErrorCode.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo(
                            "Hevy rejected the request as invalid. Hevy reported: "
                                    + "sets[0].type must be one of warmup, normal, failure, dropset");
                });
    }

    @ParameterizedTest
    @MethodSource("errorStatuses")
    void mapsUpstreamErrorsWithoutLeakingResponseBody(int status, HevyErrorCode code, String safeMessage) {
        responseStatus.set(status);
        responseBody.set("sensitive upstream details fake-api-key");

        assertThatThrownBy(() -> client.getWorkout("w1"))
                .isInstanceOfSatisfying(HevyApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(code);
                    assertThat(exception.getMessage()).isEqualTo(safeMessage);
                    assertThat(exception.getMessage()).doesNotContain("fake-api-key", "sensitive upstream details");
                });
    }

    static Stream<Arguments> errorStatuses() {
        return Stream.of(
                Arguments.of(400, HevyErrorCode.BAD_REQUEST, "Hevy rejected the request as invalid."),
                Arguments.of(401, HevyErrorCode.CREDENTIALS_REJECTED,
                        "The Hevy API rejected the configured API credentials."),
                Arguments.of(403, HevyErrorCode.FORBIDDEN, "The Hevy API denied this operation."),
                Arguments.of(404, HevyErrorCode.NOT_FOUND, "The requested Hevy workout was not found."),
                Arguments.of(429, HevyErrorCode.RATE_LIMITED,
                        "The Hevy API rate limit was reached. Try again later."),
                Arguments.of(503, HevyErrorCode.UNAVAILABLE, "The Hevy API is temporarily unavailable."));
    }

    @Test
    void rejectsMalformedResponseSafely() {
        responseBody.set("not-json");

        assertThatThrownBy(() -> client.getWorkout("w1"))
                .isInstanceOfSatisfying(HevyApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo(HevyErrorCode.INVALID_RESPONSE));
    }

    @Test
    void rejectsInvalidPaginationBeforeCallingUpstream() {
        assertThatThrownBy(() -> client.getWorkouts(0, 11))
                .isInstanceOfSatisfying(HevyApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo(HevyErrorCode.BAD_REQUEST));
        assertThat(request.get()).isNull();
    }

    @Test
    void mapsNetworkConnectionFailuresSafely() {
        server.stop(0);
        server = null;

        assertThatThrownBy(() -> client.getWorkout("w1"))
                .isInstanceOfSatisfying(HevyApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(HevyErrorCode.UNAVAILABLE);
                    assertThat(exception.getMessage()).isEqualTo("The Hevy API is temporarily unavailable.");
                });
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] incoming = exchange.getRequestBody().readAllBytes();
        Request captured = new Request(
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("api-key"),
                new String(incoming, StandardCharsets.UTF_8));
        request.set(captured);
        requests.add(captured);
        Response queued = responses.poll();
        int status = queued == null ? responseStatus.get() : queued.status();
        String body = queued == null ? responseBody.get() : queued.body();
        byte[] outgoing = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
        }
        else {
            exchange.sendResponseHeaders(status, outgoing.length);
            exchange.getResponseBody().write(outgoing);
        }
        exchange.close();
    }

    private record Request(String method, String path, String apiKey, String body) {
    }

    private record Response(int status, String body) {
    }
}
