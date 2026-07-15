package io.github.hevymcp.hevy;

import io.github.hevymcp.hevy.model.Routine;
import io.github.hevymcp.mcp.model.RoutineUpdateInput;
import org.springframework.stereotype.Service;

@Service
public final class RoutineUpdateService {

    private final HevyClient hevyClient;
    private final RoutineUpdateMapper mapper;

    public RoutineUpdateService(HevyClient hevyClient, RoutineUpdateMapper mapper) {
        this.hevyClient = hevyClient;
        this.mapper = mapper;
    }

    public Routine updateRoutine(String routineId, RoutineUpdateInput input) {
        Routine existing;
        try {
            existing = hevyClient.getRoutine(routineId);
        }
        catch (HevyApiException exception) {
            throw contextualize("Routine update preflight GET failed", routineId, exception, false);
        }
        var request = mapper.merge(existing, input);
        try {
            hevyClient.putRoutine(routineId, request);
        }
        catch (HevyApiException exception) {
            throw contextualize("Routine update PUT failed", routineId, exception, false);
        }

        try {
            return hevyClient.getRoutine(routineId);
        }
        catch (HevyApiException exception) {
            throw contextualize("Routine update PUT succeeded, but verification GET failed",
                    routineId, exception, true);
        }
    }

    private HevyApiException contextualize(
            String operation, String routineId, HevyApiException cause, boolean mayHaveSucceeded) {
        String status = cause.httpStatus() == null ? "" : " [HTTP " + cause.httpStatus() + "]";
        String uncertainty = mayHaveSucceeded ? " The update may have succeeded in Hevy." : "";
        return new HevyApiException(cause.code(),
                operation + " for routine ID " + routineId + status + ": "
                        + cause.getMessage() + uncertainty,
                cause,
                cause.httpStatus());
    }
}
