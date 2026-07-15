package io.github.hevymcp.hevy;

import io.github.hevymcp.hevy.model.HevyUpdateRoutineRequest;
import io.github.hevymcp.hevy.model.Routine;
import io.github.hevymcp.mcp.model.RoutineUpdateInput;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RoutineUpdateServiceTest {

    @Test
    void identifiesPreflightReadFailuresBeforeAnyWrite() {
        HevyClient client = mock(HevyClient.class);
        RoutineUpdateMapper mapper = mock(RoutineUpdateMapper.class);
        when(client.getRoutine("missing")).thenThrow(new HevyApiException(
                HevyErrorCode.NOT_FOUND, "The requested Hevy routine was not found.", null, 404));

        assertThatThrownBy(() -> new RoutineUpdateService(client, mapper).updateRoutine("missing", input()))
                .isInstanceOfSatisfying(HevyApiException.class, exception ->
                        assertThat(exception.getMessage()).contains(
                                "preflight GET failed", "routine ID missing", "HTTP 404"));
    }

    @Test
    void putsOnceThenGetsAndReturnsCanonicalRefreshedRoutine() {
        HevyClient client = mock(HevyClient.class);
        RoutineUpdateMapper mapper = mock(RoutineUpdateMapper.class);
        Routine existing = routine("Old", "created", "updated");
        Routine refreshed = routine("New", "created", "updated-again");
        RoutineUpdateInput input = input();
        HevyUpdateRoutineRequest request = request();
        when(client.getRoutine("r1")).thenReturn(existing, refreshed);
        when(mapper.merge(existing, input)).thenReturn(request);
        var service = new RoutineUpdateService(client, mapper);

        assertThat(service.updateRoutine("r1", input)).isSameAs(refreshed);

        InOrder order = inOrder(client);
        order.verify(client).getRoutine("r1");
        order.verify(client).putRoutine("r1", request);
        order.verify(client).getRoutine("r1");
        verifyNoMoreInteractions(client);
    }

    @Test
    void preservesPutValidationFailureWithOperationStatusAndRoutineId() {
        HevyClient client = mock(HevyClient.class);
        RoutineUpdateMapper mapper = mock(RoutineUpdateMapper.class);
        Routine existing = routine("Old", "created", "updated");
        RoutineUpdateInput input = input();
        HevyUpdateRoutineRequest request = request();
        when(client.getRoutine("r1")).thenReturn(existing);
        when(mapper.merge(existing, input)).thenReturn(request);
        doThrow(new HevyApiException(HevyErrorCode.BAD_REQUEST,
                "Hevy rejected the request as invalid. Hevy reported: notes is empty", null, 400))
                .when(client).putRoutine("r1", request);

        assertThatThrownBy(() -> new RoutineUpdateService(client, mapper).updateRoutine("r1", input))
                .isInstanceOfSatisfying(HevyApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(HevyErrorCode.BAD_REQUEST);
                    assertThat(exception.httpStatus()).isEqualTo(400);
                    assertThat(exception.getMessage()).contains(
                            "Routine update PUT failed", "routine ID r1", "HTTP 400", "notes is empty");
                });
    }

    @Test
    void reportsThatWriteMayHaveSucceededWhenVerificationGetFails() {
        HevyClient client = mock(HevyClient.class);
        RoutineUpdateMapper mapper = mock(RoutineUpdateMapper.class);
        Routine existing = routine("Old", "created", "updated");
        RoutineUpdateInput input = input();
        HevyUpdateRoutineRequest request = request();
        when(client.getRoutine("r1")).thenReturn(existing)
                .thenThrow(new HevyApiException(HevyErrorCode.UNAVAILABLE,
                        "The Hevy API is temporarily unavailable.", null, 503));
        when(mapper.merge(existing, input)).thenReturn(request);

        assertThatThrownBy(() -> new RoutineUpdateService(client, mapper).updateRoutine("r1", input))
                .isInstanceOfSatisfying(HevyApiException.class, exception ->
                        assertThat(exception.getMessage()).contains(
                                "PUT succeeded", "verification GET failed", "routine ID r1",
                                "HTTP 503", "may have succeeded"));
    }

    private Routine routine(String title, String created, String updated) {
        return new Routine("r1", title, "Notes", null, updated, created, List.of());
    }

    private RoutineUpdateInput input() {
        return new RoutineUpdateInput(new RoutineUpdateInput.RoutineUpdate("New", null, null));
    }

    private HevyUpdateRoutineRequest request() {
        return new HevyUpdateRoutineRequest(
                new HevyUpdateRoutineRequest.RoutineUpdate("New", "Notes", List.of()));
    }
}
