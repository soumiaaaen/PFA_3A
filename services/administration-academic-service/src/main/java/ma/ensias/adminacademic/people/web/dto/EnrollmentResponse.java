package ma.ensias.adminacademic.people.web.dto;

import ma.ensias.adminacademic.people.entity.GroupEnrollment;

import java.util.UUID;

public record EnrollmentResponse(UUID studentUserId, String studentNumber, String studentName,
                                 UUID groupId, String groupName, String programCode,
                                 Integer studyLevel, String academicYearLabel) {

    public static EnrollmentResponse from(GroupEnrollment e) {
        var user = e.getStudent().getUser();
        var group = e.getStudentGroup();
        return new EnrollmentResponse(
                e.getStudent().getUserId(), e.getStudent().getStudentNumber(),
                user.getFirstName() + " " + user.getLastName(),
                group.getId(), group.getName(), group.getProgram().getCode(),
                group.getStudyLevel(), e.getAcademicYear().getLabel());
    }
}