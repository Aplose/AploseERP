package fr.aplose.erp.api.v1.web;

import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.modules.contact.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contacts")
@PreAuthorize("hasAuthority('CONTACT_READ')")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "CRM contacts")
public class ContactApiController {

    private final ContactService contactService;

    @GetMapping
    @Operation(summary = "List contacts")
    public Page<Contact> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return contactService.findAll(q, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contact by ID")
    public ResponseEntity<Contact> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(contactService.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
