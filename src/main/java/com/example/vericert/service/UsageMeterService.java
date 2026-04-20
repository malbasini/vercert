package com.example.vericert.service;

import com.example.vericert.domain.TenantSettings;
import com.example.vericert.domain.UsageMeter;
import com.example.vericert.domain.UsageMeterKey;
import com.example.vericert.dto.DailyUsageDTO;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.repo.UsageMeterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class UsageMeterService {

    private final UsageMeterRepository usageMeterRepository;
    private final StorageUsageService storageUsageService;
    private final TenantSettingsRepository tenantSettingsRepository;

    public UsageMeterService(UsageMeterRepository usageMeterRepository,
                             StorageUsageService storageUsageService,
                             TenantSettingsRepository tenantSettingsRepository) {


        this.usageMeterRepository = usageMeterRepository;
        this.storageUsageService = storageUsageService;
        this.tenantSettingsRepository = tenantSettingsRepository;
    }

    private UsageMeter getOrCreateToday(Long tenantId) {
        LocalDate today = LocalDate.now();
        return usageMeterRepository
                .findByTenantAndDay(tenantId, today)
                .orElseGet(() -> {
                    UsageMeter m = new UsageMeter(new UsageMeterKey(tenantId, today));
                    m.setCertsGenerated(0);
                    m.setApiCalls(0);
                    m.setPdfStorageMb(BigDecimal.ZERO);
                    m.setLastUpdateTs(Instant.now());
                    m.setVerificationsCount(0);
                    this.updateDaysBack(tenantId);
                    return usageMeterRepository.save(m);
                });
    }

    @Transactional
    public void incrementDocumentsGenerated(Long tenantId,long bytesGenerated) {
        UsageMeter m = getOrCreateToday(tenantId);
        m.setCertsGenerated(m.getCertsGenerated() + 1);
        m.setLastUpdateTs(Instant.now());
        //AGGIORNO LO STORAGE
        m.setPdfStorageMb(m.getPdfStorageMb().add(storageUsageService.bytesToMb(bytesGenerated)));
        this.updateDaysBack(tenantId);
        usageMeterRepository.save(m);
    }

    @Transactional
    public void decrementStorage(Long tenantId,long bytesGenerated) {
        UsageMeter m = getOrCreateToday(tenantId);
        m.setLastUpdateTs(Instant.now());
        //AGGIORNO LO STORAGE
        m.setPdfStorageMb(m.getPdfStorageMb().subtract(storageUsageService.bytesToMb(bytesGenerated)));
        this.updateDaysBack(tenantId);
        usageMeterRepository.save(m);
    }

    @Transactional
    public void incrementApiCalls(Long tenantId) {
        UsageMeter m = getOrCreateToday(tenantId);
        m.setApiCalls(m.getApiCalls() + 1);
        m.setLastUpdateTs(Instant.now());
        this.updateDaysBack(tenantId);
        usageMeterRepository.save(m);
    }
    @Transactional
    public void incrementVerifications(Long tenantId) {
        UsageMeter m = getOrCreateToday(tenantId);
        m.setVerificationsCount(m.getVerificationsCount() + 1);
        m.setLastUpdateTs(Instant.now());
        this.updateDaysBack(tenantId);
        usageMeterRepository.save(m);
    }
    @Transactional
    public List<DailyUsageDTO> getUsageHistoryForTenant(Long tenantId) {
        int daysBack = 7;
        TenantSettings ts = tenantSettingsRepository.findById(tenantId).orElseThrow();
        String currentCycle = ts.getBillingCycle();
        if (currentCycle.equals("ANNUAL")) {
            daysBack = 365;
        }
        if (currentCycle.equals("MONTHLY")) {
            daysBack = 30;
        }
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(daysBack);
        this.updateDaysBack(tenantId);
        return usageMeterRepository.getUsageHistoryForTenant(tenantId, from, to);
    }

    @Transactional(readOnly = true)
    public List<DailyUsageDTO> getTopTenantsToday() {
        LocalDate today = LocalDate.now();
        return usageMeterRepository.getTopTenantsToday(today);
    }

    public void updateDaysBack(Long tenantId) {
        var daysBack = 7;
        TenantSettings ts = tenantSettingsRepository.findById(tenantId).orElseThrow();
        String currentCycle = ts.getBillingCycle();
        if (currentCycle.equals("ANNUAL")) {
            daysBack = 365;
        }
        if (currentCycle.equals("MONTHLY")) {
            daysBack = 30;
        }
        if (usageMeterRepository.count() > 0)
            usageMeterRepository.updateDaysBack(tenantId, daysBack);
    }








}
