package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memora.common.exception.BusinessException;
import com.memora.manager.dto.KnowledgeBaseCreateDTO;
import com.memora.manager.entity.Document;
import com.memora.manager.dto.KnowledgeBaseUpdateDTO;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.entity.Tenant;
import com.memora.manager.entity.TenantMember;
import com.memora.manager.mapper.DocumentMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.mapper.TenantMapper;
import com.memora.manager.support.AuditLogCommand;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.SlugUtils;
import com.memora.manager.support.TenantAccessService;
import com.memora.manager.vo.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> {
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final TenantMapper tenantMapper;
    private final CurrentAccessContext currentAccessContext;
    private final TenantAccessService tenantAccessService;
    private final AuditLogService auditLogService;

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO create(KnowledgeBaseCreateDTO dto) {
        Long tenantId = resolveAccessibleTenantId(dto.getTenantId());
        ensureTenantExists(tenantId);
        TenantMember actor = tenantAccessService.requireEditor(tenantId);

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(dto, knowledgeBase);
        knowledgeBase.setTenantId(tenantId);
        knowledgeBase.setUserId(currentAccessContext.getCurrentUserId());
        knowledgeBase.setSlug(resolveKnowledgeBaseSlug(dto.getName(), dto.getSlug()));
        knowledgeBase.setStatus(1);
        knowledgeBase.setDocumentCount(0);
        knowledgeBase.setViewCount(0);
        knowledgeBase.setSortOrder(0);
        knowledgeBase.setCreatedAt(LocalDateTime.now());
        knowledgeBase.setUpdatedAt(LocalDateTime.now());

        this.save(knowledgeBase);
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(tenantId)
            .knowledgeBaseId(knowledgeBase.getId())
            .knowledgeBaseName(knowledgeBase.getName())
            .actorUserId(actor.getUserId())
            .actorRole(actor.getRole())
            .objectType(AuditLogConstants.OBJECT_KNOWLEDGE_BASE)
            .objectId(knowledgeBase.getId())
            .objectTitle(knowledgeBase.getName())
            .actionType(AuditLogConstants.ACTION_CREATE_KNOWLEDGE_BASE)
            .detail("创建知识库")
            .build());
        return convertToVO(knowledgeBase);
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO update(Long id, KnowledgeBaseUpdateDTO dto) {
        KnowledgeBase knowledgeBase = getAccessibleEntity(id);
        String actorRole = tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);
        List<String> changedFields = new ArrayList<>();
        String oldName = knowledgeBase.getName();
        String oldSlug = knowledgeBase.getSlug();
        String oldDescription = knowledgeBase.getDescription();
        String oldCover = knowledgeBase.getCover();
        if (StringUtils.hasText(dto.getName())) {
            knowledgeBase.setName(dto.getName());
            if (!StringUtils.hasText(dto.getSlug())) {
                knowledgeBase.setSlug(resolveKnowledgeBaseSlug(dto.getName(), null));
            }
        }
        if (StringUtils.hasText(dto.getSlug())) {
            knowledgeBase.setSlug(resolveKnowledgeBaseSlug(knowledgeBase.getName(), dto.getSlug()));
        }
        if (dto.getDescription() != null) {
            knowledgeBase.setDescription(dto.getDescription());
        }
        if (dto.getCover() != null) {
            knowledgeBase.setCover(dto.getCover());
        }

        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        this.updateById(knowledgeBase);
        if (!Objects.equals(oldName, knowledgeBase.getName())) {
            changedFields.add("名称");
        }
        if (!Objects.equals(oldSlug, knowledgeBase.getSlug())) {
            changedFields.add("标识");
        }
        if (!Objects.equals(oldDescription, knowledgeBase.getDescription())) {
            changedFields.add("描述");
        }
        if (!Objects.equals(oldCover, knowledgeBase.getCover())) {
            changedFields.add("封面");
        }
        if (!changedFields.isEmpty()) {
            auditLogService.recordSuccess(AuditLogCommand.builder()
                .tenantId(knowledgeBase.getTenantId())
                .knowledgeBaseId(knowledgeBase.getId())
                .knowledgeBaseName(knowledgeBase.getName())
                .actorUserId(currentAccessContext.getCurrentUserId())
                .actorRole(actorRole)
                .objectType(AuditLogConstants.OBJECT_KNOWLEDGE_BASE)
                .objectId(knowledgeBase.getId())
                .objectTitle(knowledgeBase.getName())
                .actionType(AuditLogConstants.ACTION_UPDATE_KNOWLEDGE_BASE)
                .detail("更新知识库字段：" + String.join("、", changedFields))
                .build());
        }
        return convertToVO(knowledgeBase);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        KnowledgeBase knowledgeBase = getAccessibleEntity(id);
        String actorRole = tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);
        knowledgeBase.setStatus(0);
        knowledgeBase.setDeletedAt(LocalDateTime.now());
        knowledgeBase.setDeletedBy(currentAccessContext.getCurrentUserId());
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        this.updateById(knowledgeBase);
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(knowledgeBase.getTenantId())
            .knowledgeBaseId(knowledgeBase.getId())
            .knowledgeBaseName(knowledgeBase.getName())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_KNOWLEDGE_BASE)
            .objectId(knowledgeBase.getId())
            .objectTitle(knowledgeBase.getName())
            .actionType(AuditLogConstants.ACTION_DELETE_KNOWLEDGE_BASE)
            .detail("删除知识库并保留回收站恢复入口")
            .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO restore(Long id) {
        KnowledgeBase knowledgeBase = getDeletedAccessibleEntity(id);
        String actorRole = tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);
        knowledgeBase.setStatus(1);
        knowledgeBase.setDeletedAt(null);
        knowledgeBase.setDeletedBy(null);
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        this.updateById(knowledgeBase);
        knowledgeBaseMapper.updateDocumentCount(knowledgeBase.getId(), countActiveDocumentsByKnowledgeBaseId(knowledgeBase.getId()));
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(knowledgeBase.getTenantId())
            .knowledgeBaseId(knowledgeBase.getId())
            .knowledgeBaseName(knowledgeBase.getName())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_KNOWLEDGE_BASE)
            .objectId(knowledgeBase.getId())
            .objectTitle(knowledgeBase.getName())
            .actionType(AuditLogConstants.ACTION_RESTORE_KNOWLEDGE_BASE)
            .detail("从回收站恢复知识库")
            .build());
        return convertToVO(knowledgeBase);
    }

    public KnowledgeBaseVO getById(Long id) {
        KnowledgeBase knowledgeBase = getAccessibleEntity(id);
        tenantAccessService.requireKnowledgeBaseReadAccess(knowledgeBase);
        return convertToVO(knowledgeBase);
    }

    public IPage<KnowledgeBaseVO> list(Integer page, Integer size, String keyword, Long tenantId, Long userId) {
        Page<KnowledgeBase> pageParam = new Page<>(page, size);
        Long accessibleTenantId = resolveAccessibleTenantId(tenantId);
        tenantAccessService.requireTenantMember(accessibleTenantId);
        List<KnowledgeBase> accessibleKnowledgeBases = listAccessibleKnowledgeBases(keyword, accessibleTenantId, userId);
        IPage<KnowledgeBaseVO> voPage = buildKnowledgeBasePage(page, size, accessibleKnowledgeBases);
        return voPage;
    }

    public List<KnowledgeBaseVO> listByTenantId(Long tenantId) {
        Long accessibleTenantId = resolveAccessibleTenantId(tenantId);
        tenantAccessService.requireTenantMember(accessibleTenantId);
        return listAccessibleKnowledgeBases(null, accessibleTenantId, null).stream().map(this::convertToVO).collect(Collectors.toList());
    }

    public List<KnowledgeBaseVO> listByUserId(Long userId) {
        Long currentTenantId = currentAccessContext.getCurrentTenantId();
        tenantAccessService.requireTenantMember(currentTenantId);
        return listAccessibleKnowledgeBases(null, currentTenantId, userId).stream().map(this::convertToVO).collect(Collectors.toList());
    }

    public List<KnowledgeBaseVO> listDeleted() {
        Long currentTenantId = currentAccessContext.getCurrentTenantId();
        tenantAccessService.requireTenantMember(currentTenantId);
        List<KnowledgeBase> deletedKnowledgeBases = listKnowledgeBasesByStatus(null, currentTenantId, null, 0);
        return tenantAccessService.filterManageableKnowledgeBases(deletedKnowledgeBases).stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    private LambdaQueryWrapper<KnowledgeBase> buildQuery(String keyword, Long tenantId, Long userId, Integer status) {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getStatus, status);

        if (tenantId != null) {
            queryWrapper.eq(KnowledgeBase::getTenantId, tenantId);
        }
        if (userId != null) {
            queryWrapper.eq(KnowledgeBase::getUserId, userId);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like(KnowledgeBase::getName, keyword)
                .or()
                .like(KnowledgeBase::getDescription, keyword));
        }
        return queryWrapper;
    }

    private List<KnowledgeBase> listAccessibleKnowledgeBases(String keyword, Long tenantId, Long userId) {
        return tenantAccessService.filterReadableKnowledgeBases(listKnowledgeBasesByStatus(keyword, tenantId, userId, 1));
    }

    private List<KnowledgeBase> listKnowledgeBasesByStatus(String keyword, Long tenantId, Long userId, Integer status) {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = buildQuery(keyword, tenantId, userId, status);
        if (status != null && status == 0) {
            queryWrapper.orderByDesc(KnowledgeBase::getDeletedAt).orderByDesc(KnowledgeBase::getUpdatedAt);
        } else {
            queryWrapper.orderByAsc(KnowledgeBase::getSortOrder).orderByDesc(KnowledgeBase::getUpdatedAt);
        }
        return this.list(queryWrapper);
    }

    private IPage<KnowledgeBaseVO> buildKnowledgeBasePage(Integer page, Integer size, List<KnowledgeBase> accessibleKnowledgeBases) {
        int fromIndex = Math.max((page - 1) * size, 0);
        int toIndex = Math.min(fromIndex + size, accessibleKnowledgeBases.size());
        List<KnowledgeBaseVO> pageRecords = fromIndex >= accessibleKnowledgeBases.size()
            ? List.of()
            : accessibleKnowledgeBases.subList(fromIndex, toIndex).stream().map(this::convertToVO).collect(Collectors.toList());

        Page<KnowledgeBaseVO> voPage = new Page<>(page, size, accessibleKnowledgeBases.size());
        voPage.setRecords(pageRecords);
        return voPage;
    }

    private KnowledgeBase getActiveEntity(Long id) {
        KnowledgeBase knowledgeBase = super.getById(id);
        if (knowledgeBase == null || knowledgeBase.getStatus() == null || knowledgeBase.getStatus() == 0) {
            throw new BusinessException(404, "知识库不存在");
        }
        return knowledgeBase;
    }

    private KnowledgeBase getAccessibleEntity(Long id) {
        KnowledgeBase knowledgeBase = getActiveEntity(id);
        if (!knowledgeBase.getTenantId().equals(currentAccessContext.getCurrentTenantId())) {
            throw new BusinessException(403, "无权访问该知识库");
        }
        return knowledgeBase;
    }

    private KnowledgeBase getDeletedAccessibleEntity(Long id) {
        KnowledgeBase knowledgeBase = super.getById(id);
        if (knowledgeBase == null || knowledgeBase.getStatus() == null || knowledgeBase.getStatus() != 0) {
            throw new BusinessException(404, "知识库不在回收站中");
        }
        if (!knowledgeBase.getTenantId().equals(currentAccessContext.getCurrentTenantId())) {
            throw new BusinessException(403, "无权访问该知识库");
        }
        return knowledgeBase;
    }

    private Long resolveAccessibleTenantId(Long tenantId) {
        Long currentTenantId = currentAccessContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(currentTenantId)) {
            throw new BusinessException(403, "无权访问当前租户数据");
        }
        return currentTenantId;
    }

    private void ensureTenantExists(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getStatus() == null || tenant.getStatus() == 0) {
            throw new BusinessException(404, "租户不存在");
        }
    }

    private String resolveKnowledgeBaseSlug(String name, String slug) {
        return StringUtils.hasText(slug) ? SlugUtils.toSlug(slug) : SlugUtils.toSlug(name);
    }

    private long countActiveDocumentsByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getStatus, 1)
            .eq(Document::getDocType, "DOC");
        return documentMapper.selectCount(queryWrapper);
    }

    private KnowledgeBaseVO convertToVO(KnowledgeBase knowledgeBase) {
        return convertToVO(knowledgeBase, true);
    }

    private KnowledgeBaseVO convertToVO(KnowledgeBase knowledgeBase, boolean includePermissionContext) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, vo);
        if (includePermissionContext) {
            String currentRole = tenantAccessService.getKnowledgeBaseRole(knowledgeBase);
            vo.setCurrentRole(currentRole);
            vo.setCanWrite(tenantAccessService.canWriteKnowledgeBase(currentRole));
            vo.setCanManage(tenantAccessService.canManageKnowledgeBase(currentRole));
            vo.setPermissionRestricted(tenantAccessService.hasKnowledgeBaseRestrictions(knowledgeBase.getId()));
        }
        return vo;
    }
}
