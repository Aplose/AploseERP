package fr.aplose.erp.api.v1.web;

import fr.aplose.erp.modules.commerce.entity.SalesOrder;
import fr.aplose.erp.modules.commerce.service.SalesOrderService;
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
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasAuthority('SALES_ORDER_READ')")
@RequiredArgsConstructor
@Tag(name = "Sales orders", description = "Sales orders")
public class SalesOrderApiController {

    private final SalesOrderService salesOrderService;

    @GetMapping
    @Operation(summary = "List sales orders")
    public Page<SalesOrder> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return salesOrderService.findAll(q, status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sales order by ID")
    public ResponseEntity<SalesOrder> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(salesOrderService.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
