package ma.ensias.adminacademic.audit.web;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.audit.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService service;

    @GetMapping
    public List<AuditEventResponse> list(@RequestParam(required = false) String entityType,
                                         @RequestParam(required = false) UUID entityId,
                                         @RequestParam(defaultValue = "50") int limit) {
        return service.list(entityType, entityId, Math.min(Math.max(limit, 1), 200));
    }
}