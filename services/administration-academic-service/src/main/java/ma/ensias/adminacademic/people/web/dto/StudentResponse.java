package ma.ensias.adminacademic.people.web.dto;

import ma.ensias.adminacademic.people.entity.Student;

import java.util.UUID;

public record StudentResponse(UUID userId, String studentNumber,
                              String firstName, String lastName, String email) {

    public static StudentResponse from(Student s) {
        return new StudentResponse(s.getUserId(), s.getStudentNumber(),
                s.getUser().getFirstName(), s.getUser().getLastName(), s.getUser().getEmail());
    }
}