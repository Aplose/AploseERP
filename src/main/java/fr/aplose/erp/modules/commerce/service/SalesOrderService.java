package fr.aplose.erp.modules.commerce.service;

import fr.aplose.erp.modules.catalog.repository.ProductRepository;
import fr.aplose.erp.modules.commerce.entity.Proposal;
import fr.aplose.erp.modules.commerce.entity.SalesOrder;
import fr.aplose.erp.modules.commerce.entity.SalesOrderLine;
import fr.aplose.erp.modules.commerce.repository.ProposalRepository;
import fr.aplose.erp.modules.commerce.repository.SalesOrderRepository;
import fr.aplose.erp.modules.commerce.web.dto.LineDto;
import fr.aplose.erp.modules.commerce.web.dto.SalesOrderDto;
import fr.aplose.erp.modules.contact.repository.ContactRepository;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SalesOrderService {

    private final SalesOrderRepository orderRepo;
    private final ThirdPartyRepository thirdPartyRepo;
    private final ContactRepository contactRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final ProposalRepository proposalRepo;

    @Transactional(readOnly = true)
    public Page<SalesOrder> findAll(String q, String status, Pageable pageable) {
        String tid = TenantContext.getCurrentTenantId();
        if (q != null && !q.isBlank()) return orderRepo.search(tid, q, pageable);
        if (status != null && !status.isBlank()) return orderRepo.findByTenantIdAndStatus(tid, status, pageable);
        return orderRepo.findByTenantId(tid, pageable);
    }

    @Transactional(readOnly = true)
    public SalesOrder findById(Long id) {
        return orderRepo.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    @Transactional
    public SalesOrder create(SalesOrderDto dto, Long currentUserId) {
        String tid = TenantContext.getCurrentTenantId();
        SalesOrder o = new SalesOrder();
        o.setReference(generateReference(tid));
        o.setCreatedById(currentUserId);
        o.setStatus("CONFIRMED");
        applyDto(o, dto, tid);
        o.setDateOrdered(dto.getDateOrdered());
        o.setDateExpected(dto.getDateExpected());
        o.setExchangeRate(BigDecimal.ONE);
        return orderRepo.save(o);
    }

    @Transactional
    public SalesOrder createFromProposal(Long proposalId, Long currentUserId) {
        Proposal p = proposalRepo.findByIdAndTenantId(proposalId, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        if (!"ACCEPTED".equals(p.getStatus())) {
            throw new IllegalStateException("Only accepted proposals can be converted to orders");
        }
        String tid = TenantContext.getCurrentTenantId();
        SalesOrder o = new SalesOrder();
        o.setReference(generateReference(tid));
        o.setCreatedById(currentUserId);
        o.setProposal(p);
        o.setThirdParty(p.getThirdParty());
        o.setContact(p.getContact());
        o.setStatus("CONFIRMED");
        o.setDateOrdered(java.time.LocalDate.now());
        o.setCurrencyCode(p.getCurrencyCode());
        o.setExchangeRate(p.getExchangeRate());
        o.setDiscountAmount(p.getDiscountAmount());
        o.setNotes(p.getNotes());
        o.setTerms(p.getTerms());
        o.setSalesRep(p.getSalesRep());
        o.setSubtotal(p.getSubtotal());
        o.setVatAmount(p.getVatAmount());
        o.setTotalAmount(p.getTotalAmount());
        short sortOrder = 0;
        for (var pl : p.getLines()) {
            SalesOrderLine line = new SalesOrderLine();
            line.setTenantId(tid);
            line.setOrder(o);
            line.setProduct(pl.getProduct());
            line.setSortOrder(sortOrder++);
            line.setDescription(pl.getDescription());
            line.setQuantity(pl.getQuantity());
            line.setUnitPrice(pl.getUnitPrice());
            line.setDiscountPct(pl.getDiscountPct());
            line.setVatRate(pl.getVatRate());
            line.recalculate();
            o.getLines().add(line);
        }
        o.recalculate();
        return orderRepo.save(o);
    }

    @Transactional
    public SalesOrder update(Long id, SalesOrderDto dto) {
        SalesOrder o = findById(id);
        if ("CANCELLED".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus())) {
            throw new IllegalStateException("Order cannot be edited in current status");
        }
        applyDto(o, dto, TenantContext.getCurrentTenantId());
        o.setDateOrdered(dto.getDateOrdered());
        o.setDateExpected(dto.getDateExpected());
        o.recalculate();
        return orderRepo.save(o);
    }

    @Transactional
    public SalesOrderLine addLine(Long orderId, LineDto dto) {
        SalesOrder o = findById(orderId);
        if ("CANCELLED".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus())) {
            throw new IllegalStateException("Cannot add lines to this order");
        }
        String tid = TenantContext.getCurrentTenantId();
        SalesOrderLine line = new SalesOrderLine();
        line.setTenantId(tid);
        line.setOrder(o);
        line.setDescription(dto.getDescription());
        line.setQuantity(dto.getQuantity());
        line.setUnitPrice(dto.getUnitPrice());
        line.setDiscountPct(dto.getDiscountPct() != null ? dto.getDiscountPct() : BigDecimal.ZERO);
        line.setVatRate(dto.getVatRate() != null ? dto.getVatRate() : BigDecimal.ZERO);
        line.setSortOrder((short) o.getLines().size());
        if (dto.getProductId() != null) {
            productRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getProductId(), tid).ifPresent(line::setProduct);
        }
        line.recalculate();
        o.getLines().add(line);
        o.recalculate();
        orderRepo.save(o);
        return line;
    }

    @Transactional
    public void removeLine(Long orderId, Long lineId) {
        SalesOrder o = findById(orderId);
        if ("CANCELLED".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus())) {
            throw new IllegalStateException("Cannot remove lines from this order");
        }
        o.getLines().removeIf(l -> l.getId().equals(lineId));
        o.recalculate();
        orderRepo.save(o);
    }

    @Transactional
    public void updateStatus(Long id, String newStatus) {
        SalesOrder o = findById(id);
        o.setStatus(newStatus);
        orderRepo.save(o);
    }

    private String generateReference(String tid) {
        int max = orderRepo.findMaxReferenceNumber(tid);
        return String.format("SO-%05d", max + 1);
    }

    private void applyDto(SalesOrder o, SalesOrderDto dto, String tid) {
        thirdPartyRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getThirdPartyId(), tid)
                .ifPresent(o::setThirdParty);
        if (dto.getContactId() != null) {
            contactRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getContactId(), tid).ifPresent(o::setContact);
        } else {
            o.setContact(null);
        }
        if (dto.getProposalId() != null) {
            proposalRepo.findByIdAndTenantId(dto.getProposalId(), tid).ifPresent(o::setProposal);
        } else {
            o.setProposal(null);
        }
        o.setCurrencyCode(dto.getCurrencyCode());
        o.setDiscountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO);
        o.setNotes(dto.getNotes());
        o.setTerms(dto.getTerms());
        if (dto.getSalesRepId() != null) {
            userRepo.findByIdAndTenantId(dto.getSalesRepId(), tid).ifPresent(o::setSalesRep);
        } else {
            o.setSalesRep(null);
        }
    }
}
