package fr.aplose.erp.modules.commerce.service;

import fr.aplose.erp.modules.commerce.entity.PipelineStage;
import fr.aplose.erp.modules.commerce.entity.Proposal;
import fr.aplose.erp.modules.commerce.repository.PipelineStageRepository;
import fr.aplose.erp.modules.commerce.repository.ProposalRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineStageRepository stageRepository;
    private final ProposalRepository proposalRepository;

    private static final List<String> OPEN_PIPELINE_STATUSES = List.of("DRAFT", "SENT", "ACCEPTED");

    @Transactional(readOnly = true)
    public List<PipelineStage> findAllStages() {
        return stageRepository.findByTenantIdOrderBySortOrderAsc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public List<Proposal> findProposalsByStage(Long stageId) {
        String tid = TenantContext.getCurrentTenantId();
        if (stageId == null) {
            return proposalRepository.findByTenantIdAndPipelineStageIdIsNullOrderByDateIssuedDesc(tid);
        }
        return proposalRepository.findByTenantIdAndPipelineStageIdOrderByDateIssuedDesc(tid, stageId);
    }

    @Transactional(readOnly = true)
    public List<Proposal> findOpenProposalsByStage(Long stageId) {
        List<Proposal> all = findProposalsByStage(stageId);
        return all.stream().filter(p -> OPEN_PIPELINE_STATUSES.contains(p.getStatus())).toList();
    }

    @Transactional
    public void moveProposalToStage(Long proposalId, Long stageId) {
        Proposal p = proposalRepository.findByIdAndTenantId(proposalId, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found"));
        if (stageId != null) {
            PipelineStage stage = stageRepository.findById(stageId)
                    .filter(s -> s.getTenantId().equals(TenantContext.getCurrentTenantId()))
                    .orElseThrow(() -> new IllegalArgumentException("Stage not found"));
            p.setPipelineStage(stage);
        } else {
            p.setPipelineStage(null);
        }
        proposalRepository.save(p);
    }

    /** Forecast: sum of (totalAmount * probability/100) for proposals in open stages (non-closed). */
    @Transactional(readOnly = true)
    public BigDecimal getWeightedForecast() {
        String tid = TenantContext.getCurrentTenantId();
        List<PipelineStage> stages = stageRepository.findByTenantIdOrderBySortOrderAsc(tid);
        BigDecimal sum = BigDecimal.ZERO;
        for (PipelineStage stage : stages) {
            if (stage.isClosed()) continue;
            List<Proposal> proposals = proposalRepository.findByTenantIdAndPipelineStageIdOrderByDateIssuedDesc(tid, stage.getId());
            for (Proposal p : proposals) {
                if (!OPEN_PIPELINE_STATUSES.contains(p.getStatus())) continue;
                BigDecimal weight = stage.getProbability() != null ? stage.getProbability().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                BigDecimal amount = p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO;
                sum = sum.add(amount.multiply(weight));
            }
        }
        List<Proposal> noStage = proposalRepository.findByTenantIdAndPipelineStageIdIsNullOrderByDateIssuedDesc(tid);
        BigDecimal defaultWeight = new BigDecimal("0.10");
        for (Proposal p : noStage) {
            if (!OPEN_PIPELINE_STATUSES.contains(p.getStatus())) continue;
            BigDecimal amount = p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO;
            sum = sum.add(amount.multiply(defaultWeight));
        }
        return sum;
    }

    /** Count by stage for indicators (open proposals only). */
    @Transactional(readOnly = true)
    public Map<Long, Long> getProposalCountByStage() {
        String tid = TenantContext.getCurrentTenantId();
        Map<Long, Long> map = new LinkedHashMap<>();
        map.put(null, (long) findOpenProposalsByStage(null).size());
        for (PipelineStage stage : stageRepository.findByTenantIdOrderBySortOrderAsc(tid)) {
            map.put(stage.getId(), (long) findOpenProposalsByStage(stage.getId()).size());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public PipelineStage getStageById(Long id) {
        if (id == null) return null;
        return stageRepository.findById(id)
                .filter(s -> s.getTenantId().equals(TenantContext.getCurrentTenantId()))
                .orElse(null);
    }

    @Transactional
    public PipelineStage saveStage(PipelineStage stage) {
        return stageRepository.save(stage);
    }

    @Transactional
    public void deleteStage(Long id) {
        stageRepository.findById(id)
                .filter(s -> s.getTenantId().equals(TenantContext.getCurrentTenantId()))
                .ifPresent(stageRepository::delete);
    }
}
