package ma.ensias.adminacademic.academic.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.academic.entity.AcademicYear;
import ma.ensias.adminacademic.academic.entity.Period;
import ma.ensias.adminacademic.academic.entity.Semester;
import ma.ensias.adminacademic.academic.repository.AcademicYearRepository;
import ma.ensias.adminacademic.academic.repository.PeriodRepository;
import ma.ensias.adminacademic.academic.repository.SemesterRepository;
import ma.ensias.adminacademic.academic.web.dto.*;
import ma.ensias.adminacademic.common.exception.BadRequestException;
import ma.ensias.adminacademic.common.exception.ConflictException;
import ma.ensias.adminacademic.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademicCalendarService {

    private final AcademicYearRepository yearRepository;
    private final SemesterRepository semesterRepository;
    private final PeriodRepository periodRepository;

    // ---------- Années ----------

    @Transactional
    public AcademicYearResponse createYear(CreateAcademicYearRequest req) {
        if (!req.endDate().isAfter(req.startDate())) {
            throw new BadRequestException("La date de fin doit être après la date de début");
        }
        if (yearRepository.existsByLabel(req.label())) {
            throw new ConflictException("L'année " + req.label() + " existe déjà");
        }
        AcademicYear year = new AcademicYear();
        year.setLabel(req.label());
        year.setStartDate(req.startDate());
        year.setEndDate(req.endDate());
        return AcademicYearResponse.from(yearRepository.save(year));
    }

    @Transactional(readOnly = true)
    public List<AcademicYearResponse> listYears() {
        return yearRepository.findAllByOrderByStartDateDesc().stream()
                .map(AcademicYearResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AcademicYearResponse getYear(UUID id) {
        return AcademicYearResponse.from(getYearEntity(id));
    }

    // ---------- Semestres et périodes ----------

    @Transactional
    public SemesterResponse createSemester(UUID yearId, CreateSemesterRequest req) {
        AcademicYear year = getYearEntity(yearId);

        if (semesterRepository.existsByAcademicYearIdAndNumber(yearId, req.number())) {
            throw new ConflictException("Le semestre S" + req.number() + " existe déjà pour " + year.getLabel());
        }
        validateSemesterDates(year, req);

        Semester semester = new Semester();
        semester.setAcademicYear(year);
        semester.setNumber(req.number());
        semester.setStartDate(req.startDate());
        semester.setEndDate(req.endDate());

        // Règle métier : exactement deux périodes par semestre, créées automatiquement
        semester.addPeriod(newPeriod(1, req.startDate(), req.period1EndDate()));
        semester.addPeriod(newPeriod(2, req.period1EndDate().plusDays(1), req.endDate()));

        return SemesterResponse.from(semesterRepository.save(semester));
    }

    @Transactional(readOnly = true)
    public List<SemesterResponse> listSemesters(UUID yearId) {
        getYearEntity(yearId);
        return semesterRepository.findByAcademicYearIdOrderByNumber(yearId).stream()
                .map(SemesterResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PeriodResponse getPeriod(UUID id) {
        return periodRepository.findById(id)
                .map(PeriodResponse::from)
                .orElseThrow(() -> new NotFoundException("Période introuvable : " + id));
    }

    // ---------- Outils privés ----------

    private AcademicYear getYearEntity(UUID id) {
        return yearRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Année introuvable : " + id));
    }

    private void validateSemesterDates(AcademicYear year, CreateSemesterRequest req) {
        if (!req.endDate().isAfter(req.startDate())) {
            throw new BadRequestException("La fin du semestre doit être après son début");
        }
        if (req.startDate().isBefore(year.getStartDate()) || req.endDate().isAfter(year.getEndDate())) {
            throw new BadRequestException("Le semestre doit être compris dans l'année " + year.getLabel());
        }
        if (!req.period1EndDate().isAfter(req.startDate()) || !req.period1EndDate().isBefore(req.endDate())) {
            throw new BadRequestException("La fin de la période 1 doit être strictement à l'intérieur du semestre");
        }
        for (Semester other : semesterRepository.findByAcademicYearIdOrderByNumber(year.getId())) {
            boolean overlap = !req.endDate().isBefore(other.getStartDate())
                    && !req.startDate().isAfter(other.getEndDate());
            if (overlap) {
                throw new ConflictException("Les dates chevauchent le semestre S" + other.getNumber());
            }
        }
    }

    private Period newPeriod(int number, LocalDate start, LocalDate end) {
        Period period = new Period();
        period.setNumber(number);
        period.setStartDate(start);
        period.setEndDate(end);
        return period;
    }
}