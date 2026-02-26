package fr.aplose.erp.api.v1.web;

import fr.aplose.erp.modules.commerce.entity.Proposal;
import fr.aplose.erp.modules.commerce.service.ProposalService;
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
@RequestMapping("/api/v1/proposals")
@PreAuthorize("hasAuthority('PROPOSAL_READ')")
@RequiredArgsConstructor
@Tag(name = "Proposals", description = "Commercial proposals / quotes")
public class ProposalApiController {

    private final ProposalService proposalService;

    @GetMapping
    @Operation(summary = "List proposals")
    public Page<Proposal> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return proposalService.findAll(q, status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get proposal by ID")
    public ResponseEntity<Proposal> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(proposalService.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
