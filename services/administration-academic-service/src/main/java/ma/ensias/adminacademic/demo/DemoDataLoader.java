package ma.ensias.adminacademic.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.ensias.adminacademic.academic.entity.*;
import ma.ensias.adminacademic.academic.repository.*;
import ma.ensias.adminacademic.assignment.entity.ActivityType;
import ma.ensias.adminacademic.assignment.entity.AssignmentHistory;
import ma.ensias.adminacademic.assignment.entity.HistoryAction;
import ma.ensias.adminacademic.assignment.entity.TeachingAssignment;
import ma.ensias.adminacademic.assignment.repository.AssignmentHistoryRepository;
import ma.ensias.adminacademic.assignment.repository.TeachingAssignmentRepository;
import ma.ensias.adminacademic.catalog.entity.Element;
import ma.ensias.adminacademic.catalog.entity.TeachingModule;
import ma.ensias.adminacademic.catalog.repository.ElementRepository;
import ma.ensias.adminacademic.catalog.repository.TeachingModuleRepository;
import ma.ensias.adminacademic.identity.entity.RoleCode;
import ma.ensias.adminacademic.identity.entity.User;
import ma.ensias.adminacademic.identity.repository.RoleRepository;
import ma.ensias.adminacademic.identity.repository.UserRepository;
import ma.ensias.adminacademic.people.entity.GroupEnrollment;
import ma.ensias.adminacademic.people.entity.Student;
import ma.ensias.adminacademic.people.repository.GroupEnrollmentRepository;
import ma.ensias.adminacademic.people.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

/**
 * Données du scénario de démonstration (§9).
 * Actif uniquement avec le profil "demo". Idempotent : ne crée que ce qui manque.
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AcademicYearRepository yearRepository;
    private final SemesterRepository semesterRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final StudentGroupRepository groupRepository;
    private final TeachingModuleRepository moduleRepository;
    private final ElementRepository elementRepository;
    private final StudentRepository studentRepository;
    private final GroupEnrollmentRepository enrollmentRepository;
    private final TeachingAssignmentRepository assignmentRepository;
    private final AssignmentHistoryRepository historyRepository;

    @Value("${app.bootstrap.admin-email}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // ---------- Comptes ----------
        User admin = user(adminEmail, adminPassword, "Admin", "ENSIAS", RoleCode.ADMIN);
        User chefDept = user("chef.dept@ensias.ma", "Chef@12345", "Karim", "Idrissi",
                RoleCode.CHEF_DEPARTEMENT, RoleCode.ENSEIGNANT);
        User chefGl = user("chef.gl@ensias.ma", "Chef@12345", "Nadia", "Tazi",
                RoleCode.CHEF_FILIERE, RoleCode.ENSEIGNANT);
        User prof = user("prof@ensias.ma", "Prof@12345", "Sara", "Alami", RoleCode.ENSEIGNANT);
        User etu1 = user("etudiant@ensias.ma", "Etu@12345", "Yassine", "Benali", RoleCode.ETUDIANT);
        User etu2 = user("etudiant2@ensias.ma", "Etu@12345", "Salma", "Ouali", RoleCode.ETUDIANT);

        // ---------- Calendrier ----------
        AcademicYear year = year("2026-2027", LocalDate.of(2026, 9, 14), LocalDate.of(2027, 7, 15));
        Semester s1 = semester(year, 1, LocalDate.of(2026, 9, 14), LocalDate.of(2027, 1, 31),
                LocalDate.of(2026, 11, 22));
        semester(year, 2, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30),
                LocalDate.of(2027, 4, 11));

        // ---------- Organisation ----------
        Department info = department("INFO", "Département Informatique", chefDept);
        Program gl = program(info, "GL", "Génie Logiciel", chefGl);
        program(info, "SSI", "Sécurité des Systèmes d'Information", null);
        StudentGroup g1 = group(gl, year, 2, "G1");
        group(gl, year, 2, "G2");

        // ---------- Catalogue ----------
        TeachingModule m1 = module(gl, "GL2-M1", "Architecture logicielle", 2);
        Element microservices = element(m1, s1.getPeriods().get(0), "GL2-M1-E1", "Microservices");
        Element patterns = element(m1, s1.getPeriods().get(1), "GL2-M1-E2", "Design patterns");

        // ---------- Étudiants ----------
        enroll(student(etu1, "E2026001"), g1);
        enroll(student(etu2, "E2026002"), g1);

        // ---------- Affectations ----------
        assignment(microservices, prof, g1, ActivityType.CM, 20, admin);
        assignment(patterns, chefGl, g1, ActivityType.CM, 18, admin);

        log.info("""
                Données de démo prêtes. Comptes :
                  ADMIN            {} / {}
                  CHEF_DEPARTEMENT chef.dept@ensias.ma / Chef@12345
                  CHEF_FILIERE GL  chef.gl@ensias.ma   / Chef@12345
                  ENSEIGNANT       prof@ensias.ma      / Prof@12345
                  ETUDIANT         etudiant@ensias.ma  / Etu@12345
                  ETUDIANT         etudiant2@ensias.ma / Etu@12345""", adminEmail, adminPassword);
    }

    // ---------- "Trouver ou créer" ----------
    // Les volumes sont petits : un findAll() filtré suffit et évite d'ajouter des méthodes aux repositories.

    private User user(String email, String password, String first, String last, RoleCode... roles) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(password));
            u.setFirstName(first);
            u.setLastName(last);
            u.setRoles(new HashSet<>(roleRepository.findByCodeIn(List.of(roles))));
            return userRepository.save(u);
        });
    }

    private AcademicYear year(String label, LocalDate start, LocalDate end) {
        return yearRepository.findAll().stream()
                .filter(y -> y.getLabel().equals(label))
                .findFirst()
                .orElseGet(() -> {
                    AcademicYear y = new AcademicYear();
                    y.setLabel(label);
                    y.setStartDate(start);
                    y.setEndDate(end);
                    return yearRepository.save(y);
                });
    }

    private Semester semester(AcademicYear year, int number, LocalDate start, LocalDate end, LocalDate p1End) {
        return semesterRepository.findByAcademicYearIdOrderByNumber(year.getId()).stream()
                .filter(s -> s.getNumber() == number)
                .findFirst()
                .orElseGet(() -> {
                    Semester s = new Semester();
                    s.setAcademicYear(year);
                    s.setNumber(number);
                    s.setStartDate(start);
                    s.setEndDate(end);
                    s.addPeriod(period(1, start, p1End));
                    s.addPeriod(period(2, p1End.plusDays(1), end));
                    return semesterRepository.save(s);
                });
    }

    private Period period(int number, LocalDate start, LocalDate end) {
        Period p = new Period();
        p.setNumber(number);
        p.setStartDate(start);
        p.setEndDate(end);
        return p;
    }

    private Department department(String code, String name, User head) {
        Department d = departmentRepository.findAll().stream()
                .filter(x -> x.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElseGet(() -> {
                    Department x = new Department();
                    x.setCode(code);
                    x.setName(name);
                    return departmentRepository.save(x);
                });
        if (head != null && d.getHeadUserId() == null) {
            d.setHeadUserId(head.getId());
        }
        return d;
    }

    private Program program(Department department, String code, String name, User head) {
        Program p = programRepository.findAll().stream()
                .filter(x -> x.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElseGet(() -> {
                    Program x = new Program();
                    x.setDepartment(department);
                    x.setCode(code);
                    x.setName(name);
                    return programRepository.save(x);
                });
        if (head != null && p.getHeadUserId() == null) {
            p.setHeadUserId(head.getId());
        }
        return p;
    }

    private StudentGroup group(Program program, AcademicYear year, int level, String name) {
        return groupRepository.findByProgramIdAndAcademicYearIdOrderByStudyLevelAscNameAsc(program.getId(), year.getId())
                .stream()
                .filter(g -> g.getStudyLevel() == level && g.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    StudentGroup g = new StudentGroup();
                    g.setProgram(program);
                    g.setAcademicYear(year);
                    g.setStudyLevel(level);
                    g.setName(name);
                    return groupRepository.save(g);
                });
    }

    private TeachingModule module(Program program, String code, String name, int level) {
        return moduleRepository.findAll().stream()
                .filter(m -> m.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElseGet(() -> {
                    TeachingModule m = new TeachingModule();
                    m.setProgram(program);
                    m.setCode(code);
                    m.setName(name);
                    m.setStudyLevel(level);
                    m.setCoefficient(new BigDecimal("1.5"));
                    return moduleRepository.save(m);
                });
    }

    private Element element(TeachingModule module, Period period, String code, String name) {
        return elementRepository.findAll().stream()
                .filter(e -> e.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElseGet(() -> {
                    Element e = new Element();
                    e.setModule(module);
                    e.setPeriod(period);
                    e.setCode(code);
                    e.setName(name);
                    e.setCoefficient(BigDecimal.ONE);
                    return elementRepository.save(e);
                });
    }

    private Student student(User user, String number) {
        return studentRepository.findById(user.getId()).orElseGet(() -> {
            Student s = new Student();
            s.setUser(user);
            s.setStudentNumber(number);
            return studentRepository.save(s);
        });
    }

    private void enroll(Student student, StudentGroup group) {
        if (!enrollmentRepository.existsByStudentUserIdAndAcademicYearId(
                student.getUserId(), group.getAcademicYear().getId())) {
            GroupEnrollment e = new GroupEnrollment();
            e.setStudent(student);
            e.setStudentGroup(group);
            e.setAcademicYear(group.getAcademicYear());
            enrollmentRepository.save(e);
        }
    }

    private void assignment(Element element, User teacher, StudentGroup group,
                            ActivityType type, int hours, User createdBy) {
        if (assignmentRepository.existsByElementIdAndTeacherIdAndStudentGroupIdAndActivityTypeAndActiveTrue(
                element.getId(), teacher.getId(), group.getId(), type)) {
            return;
        }
        TeachingAssignment a = new TeachingAssignment();
        a.setElement(element);
        a.setTeacher(teacher);
        a.setStudentGroup(group);
        a.setPeriod(element.getPeriod());
        a.setActivityType(type);
        a.setTargetHours(BigDecimal.valueOf(hours));
        a = assignmentRepository.save(a);

        AssignmentHistory h = new AssignmentHistory();
        h.setAssignment(a);
        h.setAction(HistoryAction.CREATED);
        h.setNewTeacherUserId(teacher.getId());
        h.setNewTargetHours(a.getTargetHours());
        h.setChangedBy(createdBy.getId());
        historyRepository.save(h);
    }
}