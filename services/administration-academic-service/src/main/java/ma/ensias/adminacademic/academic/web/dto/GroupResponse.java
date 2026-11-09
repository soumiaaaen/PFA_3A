package ma.ensias.adminacademic.academic.web.dto;

import ma.ensias.adminacademic.academic.entity.StudentGroup;

import java.util.UUID;

public record GroupResponse(UUID id, UUID programId, String programCode,
                            UUID academicYearId, String academicYearLabel,
                            Integer studyLevel, String name) {

    public static GroupResponse from(StudentGroup g) {
        return new GroupResponse(g.getId(),
                g.getProgram().getId(), g.getProgram().getCode(),
                g.getAcademicYear().getId(), g.getAcademicYear().getLabel(),
                g.getStudyLevel(), g.getName());
    }
}