package io.github.hevymcp.hevy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.hevymcp.config.HevyProperties;
import io.github.hevymcp.hevy.model.RoutineUpdateRequest;
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
    void serializesRoutineUpdateAndDeserializesResponse() {
        responseBody.set("""
                {"id":"r1","title":"Upper revised","exercises":[]}
                """);
        var set = new RoutineUpdateRequest.SetUpdate(
                "normal", new BigDecimal("80.5"), 8, null, null, null, null);
        var exercise = new RoutineUpdateRequest.ExerciseUpdate(
                "template-1", null, 90, "Controlled", List.of(set));
        var update = new RoutineUpdateRequest(new RoutineUpdateRequest.RoutineUpdate(
                "Upper revised", "Notes", List.of(exercise)));

        assertThat(client.updateRoutine("r1", update).title()).isEqualTo("Upper revised");
        assertThat(request.get().method()).isEqualTo("PUT");
        assertThat(request.get().path()).isEqualTo("/v1/routines/r1");
        assertThat(request.get().body())
                .contains("\"exercise_template_id\":\"template-1\"")
                .contains("\"rest_seconds\":90")
                .contains("\"weight_kg\":80.5");
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
        request.set(new Request(
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("api-key"),
                new String(incoming, StandardCharsets.UTF_8)));
        byte[] outgoing = responseBody.get().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(responseStatus.get(), outgoing.length);
        exchange.getResponseBody().write(outgoing);
        exchange.close();
    }

    private record Request(String method, String path, String apiKey, String body) {
    }
}
