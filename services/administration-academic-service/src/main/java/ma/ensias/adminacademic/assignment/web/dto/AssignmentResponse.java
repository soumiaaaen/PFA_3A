package ma.ensias.adminacademic.assignment.web.dto;

import ma.ensias.adminacademic.assignment.entity.ActivityType;
import ma.ensias.adminacademic.assignment.entity.TeachingAssignment;

import java.math.BigDecimal;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID elementId, String elementCode, String elementName, String moduleCode,
        UUID teacherUserId, String teacherName,
        UUID groupId, String groupName,
        UUID periodId, Integer periodNumber,
        ActivityType activityType, BigDecimal targetHours, boolean active) {

    public static AssignmentResponse from(TeachingAssignment a) {
        return new AssignmentResponse(
                a.getId(),
                a.getElement().getId(), a.getElement().getCode(), a.getElement().getName(),
                a.getElement().getModule().getCode(),
                a.getTeacher().getId(), a.getTeacher().getFirstName() + " " + a.getTeacher().getLastName(),
                a.getStudentGroup().getId(), a.getStudentGroup().getName(),
                a.getPeriod().getId(), a.getPeriod().getNumber(),
                a.getActivityType(), a.getTargetHours(), a.isActive());
    }
}