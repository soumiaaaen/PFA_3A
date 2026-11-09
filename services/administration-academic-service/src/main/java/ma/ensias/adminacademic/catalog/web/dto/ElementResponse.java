package ma.ensias.adminacademic.catalog.web.dto;

import ma.ensias.adminacademic.catalog.entity.Element;

import java.math.BigDecimal;
import java.util.UUID;

public record ElementResponse(UUID id, UUID moduleId, String moduleCode,
                              UUID periodId, Integer periodNumber,
                              String code, String name, String description,
                              String objectives, BigDecimal coefficient) {

    public static ElementResponse from(Element e) {
        return new ElementResponse(e.getId(),
                e.getModule().getId(), e.getModule().getCode(),
                e.getPeriod().getId(), e.getPeriod().getNumber(),
                e.getCode(), e.getName(), e.getDescription(),
                e.getObjectives(), e.getCoefficient());
    }
}