package org.monostudio.api.services.impl;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.monostudio.api.models.SupplierPojo;
import org.monostudio.api.models.inventory.SupplierUpsertRequest;
import org.monostudio.api.services.SupplierManagementService;
import org.monostudio.api.services.impl.inventory.InventoryMapper;
import org.monostudio.jpa.entities.Supplier;
import org.monostudio.jpa.repositories.SuppliersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierManagementServiceImpl
    implements SupplierManagementService {

    private final SuppliersRepository suppliersRepository;

    public SupplierManagementServiceImpl(SuppliersRepository suppliersRepository) {
        this.suppliersRepository = suppliersRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierPojo> list(String keyword) {
        return suppliersRepository.search(keyword).stream()
            .map(InventoryMapper::toPojo)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierPojo getById(Long id) {
        Supplier supplier = suppliersRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
        return InventoryMapper.toPojo(supplier);
    }

    @Override
    @Transactional
    public SupplierPojo create(SupplierUpsertRequest request) {
        validate(request, true);
        String normalizedCode = normalizeCode(request.getCode());
        if (suppliersRepository.findByCode(normalizedCode).isPresent()) {
            throw new IllegalArgumentException("Supplier code already exists: " + normalizedCode);
        }

        Supplier supplier = Supplier.builder()
            .code(normalizedCode)
            .name(request.getName().trim())
            .contactName(trimOrNull(request.getContactName()))
            .phone(trimOrNull(request.getPhone()))
            .email(trimOrNull(request.getEmail()))
            .address(trimOrNull(request.getAddress()))
            .active(request.getActive() == null || request.getActive())
            .deleted(false)
            .build();
        return InventoryMapper.toPojo(suppliersRepository.save(supplier));
    }

    @Override
    @Transactional
    public SupplierPojo update(Long id, SupplierUpsertRequest request) {
        validate(request, false);
        Supplier supplier = suppliersRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));

        if (request.getCode() != null && !request.getCode().isBlank()) {
            String normalizedCode = normalizeCode(request.getCode());
            suppliersRepository.findByCode(normalizedCode).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Supplier code already exists: " + normalizedCode);
                }
            });
            supplier.setCode(normalizedCode);
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            supplier.setName(request.getName().trim());
        }
        if (request.getContactName() != null) {
            supplier.setContactName(trimOrNull(request.getContactName()));
        }
        if (request.getPhone() != null) {
            supplier.setPhone(trimOrNull(request.getPhone()));
        }
        if (request.getEmail() != null) {
            supplier.setEmail(trimOrNull(request.getEmail()));
        }
        if (request.getAddress() != null) {
            supplier.setAddress(trimOrNull(request.getAddress()));
        }
        if (request.getActive() != null) {
            supplier.setActive(request.getActive());
        }
        return InventoryMapper.toPojo(suppliersRepository.save(supplier));
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        Supplier supplier = suppliersRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
        supplier.setDeleted(true);
        supplier.setActive(false);
        suppliersRepository.save(supplier);
    }

    private void validate(SupplierUpsertRequest request, boolean isCreate) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (isCreate && (request.getName() == null || request.getName().isBlank())) {
            throw new IllegalArgumentException("supplier name is required");
        }
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return "SUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
