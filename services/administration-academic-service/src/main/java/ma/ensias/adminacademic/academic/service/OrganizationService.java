package ma.ensias.adminacademic.academic.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.academic.entity.AcademicYear;
import ma.ensias.adminacademic.academic.entity.Department;
import ma.ensias.adminacademic.academic.entity.Program;
import ma.ensias.adminacademic.academic.entity.StudentGroup;
import ma.ensias.adminacademic.academic.repository.AcademicYearRepository;
import ma.ensias.adminacademic.academic.repository.DepartmentRepository;
import ma.ensias.adminacademic.academic.repository.ProgramRepository;
import ma.ensias.adminacademic.academic.repository.StudentGroupRepository;
import ma.ensias.adminacademic.academic.web.dto.*;
import ma.ensias.adminacademic.common.exception.BadRequestException;
import ma.ensias.adminacademic.common.exception.ConflictException;
import ma.ensias.adminacademic.common.exception.NotFoundException;
import ma.ensias.adminacademic.identity.entity.RoleCode;
import ma.ensias.adminacademic.identity.entity.User;
import ma.ensias.adminacademic.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final StudentGroupRepository groupRepository;
    private final AcademicYearRepository yearRepository;
    private final UserRepository userRepository;

    // ---------- Départements ----------

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest req) {
        String code = req.code().trim().toUpperCase();
        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Le département " + code + " existe déjà");
        }
        Department d = new Department();
        d.setCode(code);
        d.setName(req.name().trim());
        return DepartmentResponse.from(departmentRepository.save(d));
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findAllByOrderByCode().stream().map(DepartmentResponse::from).toList();
    }

    @Transactional
    public DepartmentResponse assignDepartmentHead(UUID departmentId, UUID userId) {
        Department d = getDepartment(departmentId);
        requireRole(userId, RoleCode.CHEF_DEPARTEMENT);
        d.setHeadUserId(userId);
        return DepartmentResponse.from(d);
    }

    // ---------- Filières ----------

    @Transactional
    public ProgramResponse createProgram(CreateProgramRequest req) {
        Department department = getDepartment(req.departmentId());
        String code = req.code().trim().toUpperCase();
        if (programRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("La filière " + code + " existe déjà");
        }
        Program p = new Program();
        p.setDepartment(department);
        p.setCode(code);
        p.setName(req.name().trim());
        return ProgramResponse.from(programRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<ProgramResponse> listPrograms(UUID departmentId) {
        List<Program> programs = (departmentId == null)
                ? programRepository.findAllByOrderByCode()
                : programRepository.findByDepartmentIdOrderByCode(departmentId);
        return programs.stream().map(ProgramResponse::from).toList();
    }

    @Transactional
    public ProgramResponse assignProgramHead(UUID programId, UUID userId) {
        Program p = getProgram(programId);
        requireRole(userId, RoleCode.CHEF_FILIERE);
        p.setHeadUserId(userId);
        return ProgramResponse.from(p);
    }

    // ---------- Groupes ----------

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest req) {
        Program program = getProgram(req.programId());
        AcademicYear year = yearRepository.findById(req.academicYearId())
                .orElseThrow(() -> new NotFoundException("Année introuvable : " + req.academicYearId()));
        String name = req.name().trim();

        if (groupRepository.existsByProgramIdAndAcademicYearIdAndStudyLevelAndNameIgnoreCase(
                program.getId(), year.getId(), req.studyLevel(), name)) {
            throw new ConflictException("Ce groupe existe déjà pour cette filière, cette année et ce niveau");
        }
        StudentGroup g = new StudentGroup();
        g.setProgram(program);
        g.setAcademicYear(year);
        g.setStudyLevel(req.studyLevel());
        g.setName(name);
        return GroupResponse.from(groupRepository.save(g));
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> listGroups(UUID programId, UUID academicYearId) {
        return groupRepository.findByProgramIdAndAcademicYearIdOrderByStudyLevelAscNameAsc(programId, academicYearId)
                .stream().map(GroupResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(UUID id) {
        return groupRepository.findById(id).map(GroupResponse::from)
                .orElseThrow(() -> new NotFoundException("Groupe introuvable : " + id));
    }

    // ---------- Outils privés ----------

    private Department getDepartment(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Département introuvable : " + id));
    }

    private Program getProgram(UUID id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Filière introuvable : " + id));
    }

    private void requireRole(UUID userId, RoleCode role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + userId));
        boolean hasRole = user.getRoles().stream().anyMatch(r -> r.getCode() == role);
        if (!hasRole) {
            throw new BadRequestException("L'utilisateur doit avoir le rôle " + role);
        }
    }
}