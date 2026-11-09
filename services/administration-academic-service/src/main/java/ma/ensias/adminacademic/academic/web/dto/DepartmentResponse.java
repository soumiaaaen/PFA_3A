package ma.ensias.adminacademic.academic.web.dto;

import ma.ensias.adminacademic.academic.entity.Department;

import java.util.UUID;

public record DepartmentResponse(UUID id, String code, String name, UUID headUserId) {

    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(d.getId(), d.getCode(), d.getName(), d.getHeadUserId());
    }
}