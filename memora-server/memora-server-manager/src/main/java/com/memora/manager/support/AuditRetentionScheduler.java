package com.memora.manager.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.manager.entity.Tenant;
import com.memora.manager.mapper.TenantMapper;
import com.memora.manager.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "memora.audit.retention-scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class AuditRetentionScheduler {
    private final TenantMapper tenantMapper;
    private final AuditLogService auditLogService;

    @Scheduled(
        cron = "${memora.audit.retention-scheduler.cron:0 15 2 * * *}",
        zone = "${memora.audit.retention-scheduler.zone:Asia/Shanghai}"
    )
    public void runRetention() {
        List<Long> tenantIds = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getStatus, 1))
            .stream()
            .map(Tenant::getId)
            .toList();
        long archivedCount = auditLogService.runSystemRetentionBatch(tenantIds);
        if (archivedCount > 0) {
            log.info("automatic audit retention archived {} records across {} tenants", archivedCount, tenantIds.size());
        }
    }
}
