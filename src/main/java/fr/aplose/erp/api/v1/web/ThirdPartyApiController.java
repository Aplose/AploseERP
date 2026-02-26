package fr.aplose.erp.api.v1.web;

import fr.aplose.erp.api.v1.dto.ThirdPartyResponseDto;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.modules.thirdparty.service.ThirdPartyService;
import fr.aplose.erp.modules.thirdparty.web.dto.ThirdPartyDto;
import fr.aplose.erp.security.service.ErpUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/third-parties")
@PreAuthorize("hasAuthority('THIRD_PARTY_READ')")
@RequiredArgsConstructor
@Tag(name = "Third parties", description = "CRM third parties (customers, suppliers)")
public class ThirdPartyApiController {

    private final ThirdPartyService thirdPartyService;

    @GetMapping
    @Operation(summary = "List third parties")
    public Page<ThirdPartyResponseDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filter,
            @PageableDefault(size = 20) Pageable pageable) {
        return thirdPartyService.findAll(q, filter, pageable).map(this::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get third party by ID")
    public ResponseEntity<ThirdPartyResponseDto> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(toResponse(thirdPartyService.findById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('THIRD_PARTY_CREATE')")
    @Operation(summary = "Create third party")
    public ResponseEntity<ThirdPartyResponseDto> create(
            @Valid @RequestBody ThirdPartyDto dto,
            @AuthenticationPrincipal ErpUserDetails user) {
        ThirdParty created = thirdPartyService.create(dto, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('THIRD_PARTY_UPDATE')")
    @Operation(summary = "Update third party")
    public ResponseEntity<ThirdPartyResponseDto> update(@PathVariable Long id, @Valid @RequestBody ThirdPartyDto dto) {
        ThirdParty updated = thirdPartyService.update(id, dto);
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('THIRD_PARTY_DELETE')")
    @Operation(summary = "Delete (soft) third party")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        thirdPartyService.delete(id);
    }

    private ThirdPartyResponseDto toResponse(ThirdParty tp) {
        ThirdPartyResponseDto dto = new ThirdPartyResponseDto();
        dto.setId(tp.getId());
        dto.setCode(tp.getCode());
        dto.setName(tp.getName());
        dto.setType(tp.getType());
        dto.setCustomer(tp.isCustomer());
        dto.setSupplier(tp.isSupplier());
        dto.setProspect(tp.isProspect());
        dto.setLegalForm(tp.getLegalForm());
        dto.setTaxId(tp.getTaxId());
        dto.setEmail(tp.getEmail());
        dto.setPhone(tp.getPhone());
        dto.setAddressLine1(tp.getAddressLine1());
        dto.setCity(tp.getCity());
        dto.setPostalCode(tp.getPostalCode());
        dto.setCountryCode(tp.getCountryCode());
        dto.setStatus(tp.getStatus());
        dto.setCreatedAt(tp.getCreatedAt());
        return dto;
    }
}
