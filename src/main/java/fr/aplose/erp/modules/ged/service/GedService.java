package fr.aplose.erp.modules.ged.service;

import fr.aplose.erp.modules.ged.entity.GedDocument;
import fr.aplose.erp.modules.ged.repository.GedDocumentRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GedService {

    private final GedDocumentRepository repository;

    private static final String GED_STORAGE_PREFIX = "ged";

    @Transactional(readOnly = true)
    public List<GedDocument> findByEntity(String entityType, Long entityId) {
        return repository.findByTenantIdAndEntityTypeAndEntityIdOrderByVersionDesc(
                TenantContext.getCurrentTenantId(), entityType, entityId);
    }

    @Transactional(readOnly = true)
    public Page<GedDocument> findAll(Pageable pageable) {
        return repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<GedDocument> search(String q, Pageable pageable) {
        if (q != null && !q.isBlank()) {
            return repository.searchByFileName(TenantContext.getCurrentTenantId(), q, pageable);
        }
        return findAll(pageable);
    }

    @Transactional(readOnly = true)
    public GedDocument findById(Long id) {
        return repository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<GedDocument> findByIdAndTenantId(Long id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId);
    }

    /**
     * Returns the file as a Resource for download. Caller must verify access rights.
     */
    public Resource getFileResource(GedDocument doc) throws IOException {
        Path path = Paths.get(doc.getFilePath());
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IOException("File not readable: " + doc.getFileName());
        }
        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new IOException("Invalid file path", e);
        }
    }

    @Transactional
    public GedDocument upload(String entityType, Long entityId, MultipartFile file, Long currentUserId) throws IOException {
        String tid = TenantContext.getCurrentTenantId();
        Path tenantDir = Paths.get(GED_STORAGE_PREFIX, tid).toAbsolutePath();
        Files.createDirectories(tenantDir);
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) originalName = "document";
        String storedName = UUID.randomUUID().toString() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = tenantDir.resolve(storedName);
        file.transferTo(target.toFile());

        GedDocument doc = new GedDocument();
        doc.setEntityType(entityType);
        doc.setEntityId(entityId);
        doc.setFileName(originalName);
        doc.setFilePath(target.toString());
        doc.setMimeType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setVersion(1);
        doc.setCreatedById(currentUserId);
        return repository.save(doc);
    }

    @Transactional
    public void delete(Long id) {
        GedDocument doc = findById(id);
        try {
            Path path = Paths.get(doc.getFilePath());
            if (Files.exists(path)) Files.delete(path);
        } catch (IOException ignored) { }
        repository.delete(doc);
    }
}
