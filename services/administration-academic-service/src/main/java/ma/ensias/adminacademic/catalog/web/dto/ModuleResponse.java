package ma.ensias.adminacademic.catalog.web.dto;

import ma.ensias.adminacademic.catalog.entity.TeachingModule;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ModuleResponse(UUID id, UUID programId, String programCode,
                             String code, String name, Integer studyLevel,
                             String description, String objectives, BigDecimal coefficient,
                             List<ElementResponse> elements) {

    public static ModuleResponse from(TeachingModule m) {
        return new ModuleResponse(m.getId(),
                m.getProgram().getId(), m.getProgram().getCode(),
                m.getCode(), m.getName(), m.getStudyLevel(),
                m.getDescription(), m.getObjectives(), m.getCoefficient(),
                m.getElements().stream().map(ElementResponse::from).toList());
    }
}