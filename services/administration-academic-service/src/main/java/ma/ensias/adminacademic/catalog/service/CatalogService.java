package ma.ensias.adminacademic.catalog.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.academic.entity.Period;
import ma.ensias.adminacademic.academic.entity.Program;
import ma.ensias.adminacademic.academic.repository.PeriodRepository;
import ma.ensias.adminacademic.academic.repository.ProgramRepository;
import ma.ensias.adminacademic.catalog.entity.Element;
import ma.ensias.adminacademic.catalog.entity.TeachingModule;
import ma.ensias.adminacademic.catalog.repository.ElementRepository;
import ma.ensias.adminacademic.catalog.repository.TeachingModuleRepository;
import ma.ensias.adminacademic.catalog.web.dto.*;
import ma.ensias.adminacademic.common.exception.ConflictException;
import ma.ensias.adminacademic.common.exception.NotFoundException;
import ma.ensias.adminacademic.common.security.ScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final TeachingModuleRepository moduleRepository;
    private final ElementRepository elementRepository;
    private final ProgramRepository programRepository;
    private final PeriodRepository periodRepository;
    private final ScopeService scopeService;

    // ---------- Modules ----------

    @Transactional
    public ModuleResponse createModule(CreateModuleRequest req) {
        Program program = programRepository.findById(req.programId())
                .orElseThrow(() -> new NotFoundException("Filière introuvable : " + req.programId()));
        scopeService.checkCanManageProgram(program.getHeadUserId());

        String code = req.code().trim().toUpperCase();
        if (moduleRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Le module " + code + " existe déjà");
        }
        TeachingModule m = new TeachingModule();
        m.setProgram(program);
        m.setCode(code);
        m.setName(req.name().trim());
        m.setStudyLevel(req.studyLevel());
        m.setDescription(req.description());
        m.setObjectives(req.objectives());
        m.setCoefficient(req.coefficient());
        return ModuleResponse.from(moduleRepository.save(m));
    }

    @Transactional(readOnly = true)
    public List<ModuleResponse> listModules(UUID programId) {
        return moduleRepository.findByProgramIdOrderByStudyLevelAscCodeAsc(programId).stream()
                .map(ModuleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ModuleResponse getModule(UUID id) {
        return ModuleResponse.from(getModuleEntity(id));
    }

    // ---------- Éléments ----------

    @Transactional
    public ElementResponse createElement(UUID moduleId, CreateElementRequest req) {
        TeachingModule module = getModuleEntity(moduleId);
        scopeService.checkCanManageProgram(module.getProgram().getHeadUserId());

        Period period = periodRepository.findById(req.periodId())
                .orElseThrow(() -> new NotFoundException("Période introuvable : " + req.periodId()));

        String code = req.code().trim().toUpperCase();
        if (elementRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("L'élément " + code + " existe déjà");
        }
        Element e = new Element();
        e.setModule(module);
        e.setPeriod(period);
        e.setCode(code);
        e.setName(req.name().trim());
        e.setDescription(req.description());
        e.setObjectives(req.objectives());
        e.setCoefficient(req.coefficient());
        Element saved = elementRepository.save(e);
        module.getElements().add(saved);
        return ElementResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ElementResponse getElement(UUID id) {
        return elementRepository.findById(id).map(ElementResponse::from)
                .orElseThrow(() -> new NotFoundException("Élément introuvable : " + id));
    }

    private TeachingModule getModuleEntity(UUID id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Module introuvable : " + id));
    }
}