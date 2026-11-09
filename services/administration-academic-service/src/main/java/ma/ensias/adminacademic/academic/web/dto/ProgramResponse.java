package ma.ensias.adminacademic.academic.web.dto;

import ma.ensias.adminacademic.academic.entity.Program;

import java.util.UUID;

public record ProgramResponse(UUID id, UUID departmentId, String departmentCode,
                              String code, String name, UUID headUserId) {

    public static ProgramResponse from(Program p) {
        return new ProgramResponse(p.getId(), p.getDepartment().getId(), p.getDepartment().getCode(),
                p.getCode(), p.getName(), p.getHeadUserId());
    }
}