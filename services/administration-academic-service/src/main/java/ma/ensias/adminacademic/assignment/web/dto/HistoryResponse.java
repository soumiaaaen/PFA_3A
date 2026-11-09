package ma.ensias.adminacademic.assignment.web.dto;

import ma.ensias.adminacademic.assignment.entity.AssignmentHistory;
import ma.ensias.adminacademic.assignment.entity.HistoryAction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HistoryResponse(
        HistoryAction action,
        UUID oldTeacherUserId, UUID newTeacherUserId,
        BigDecimal oldTargetHours, BigDecimal newTargetHours,
        UUID changedBy, Instant changedAt) {

    public static HistoryResponse from(AssignmentHistory h) {
        return new HistoryResponse(h.getAction(),
                h.getOldTeacherUserId(), h.getNewTeacherUserId(),
                h.getOldTargetHours(), h.getNewTargetHours(),
                h.getChangedBy(), h.getChangedAt());
    }
}