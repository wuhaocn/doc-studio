package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.dto.DocumentShareCreateDTO;
import com.memora.manager.dto.PublicShareAccessDTO;
import com.memora.manager.entity.Document;
import com.memora.manager.entity.DocumentShareLink;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.mapper.DocumentMapper;
import com.memora.manager.mapper.DocumentShareLinkMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.support.AuditLogCommand;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.OpaqueTokenCodec;
import com.memora.manager.support.OpaqueTokenGenerator;
import com.memora.manager.support.PasswordCodec;
import com.memora.manager.support.TenantAccessService;
import com.memora.manager.vo.DocumentShareLinkVO;
import com.memora.manager.vo.PublicShareDocumentVO;
import com.memora.manager.vo.PublicShareInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DocumentShareService {
    private static final int SHARE_STATUS_REVOKED = 0;
    private static final int SHARE_STATUS_ACTIVE = 1;

    @Value("${memora.share.default-expire-days:7}")
    private Integer defaultExpireDays;

    private final DocumentShareLinkMapper documentShareLinkMapper;
    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final TenantAccessService tenantAccessService;
    private final CurrentAccessContext currentAccessContext;
    private final OpaqueTokenCodec opaqueTokenCodec;
    private final OpaqueTokenGenerator opaqueTokenGenerator;
    private final PasswordCodec passwordCodec;
    private final AuditLogService auditLogService;

    public DocumentShareLinkVO createShare(DocumentShareCreateDTO dto) {
        ShareContext context = requireManageableDocument(dto.getDocumentId());
        String actorRole = tenantAccessService.requireKnowledgeBaseManageAccess(context.knowledgeBase());
        String rawShareToken = opaqueTokenGenerator.generate("share_");
        String hashedShareToken = opaqueTokenCodec.hash(rawShareToken);
        DocumentShareLink shareLink = new DocumentShareLink();
        shareLink.setTenantId(context.knowledgeBase().getTenantId());
        shareLink.setKnowledgeBaseId(context.knowledgeBase().getId());
        shareLink.setDocumentId(context.document().getId());
        shareLink.setShareToken(hashedShareToken);
        shareLink.setSecretHash(hashedShareToken);
        shareLink.setStatus(SHARE_STATUS_ACTIVE);
        shareLink.setExpiresAt(LocalDateTime.now().plusDays(resolveExpiresInDays(dto.getExpiresInDays())));
        shareLink.setAccessCodeHash(StringUtils.hasText(dto.getAccessCode()) ? passwordCodec.hash(dto.getAccessCode().trim()) : null);
        shareLink.setCreatedByUserId(currentAccessContext.getCurrentUserId());
        shareLink.setCreatedAt(LocalDateTime.now());
        shareLink.setUpdatedAt(LocalDateTime.now());
        documentShareLinkMapper.insert(shareLink);

        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(context.knowledgeBase().getTenantId())
            .knowledgeBaseId(context.knowledgeBase().getId())
            .knowledgeBaseName(context.knowledgeBase().getName())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_DOCUMENT_SHARE_LINK)
            .objectId(shareLink.getId())
            .objectTitle(context.document().getTitle())
            .actionType(AuditLogConstants.ACTION_CREATE_DOCUMENT_SHARE)
            .detail("创建文档受控分享" + (shareLink.getAccessCodeHash() != null ? "，需访问码" : ""))
            .build());
        return convertToVO(shareLink, context.document(), rawShareToken);
    }

    public List<DocumentShareLinkVO> listShares(Long documentId) {
        ShareContext context = requireManageableDocument(documentId);
        LambdaQueryWrapper<DocumentShareLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocumentShareLink::getTenantId, context.knowledgeBase().getTenantId())
            .eq(DocumentShareLink::getDocumentId, documentId)
            .orderByDesc(DocumentShareLink::getCreatedAt)
            .orderByDesc(DocumentShareLink::getId);
        return documentShareLinkMapper.selectList(queryWrapper).stream()
            .map(item -> convertToVO(item, context.document(), null))
            .toList();
    }

    public void revokeShare(Long shareId) {
        DocumentShareLink shareLink = requireTenantShare(shareId);
        ShareContext context = requireManageableDocument(shareLink.getDocumentId());
        String actorRole = tenantAccessService.requireKnowledgeBaseManageAccess(context.knowledgeBase());
        if (shareLink.getStatus() == SHARE_STATUS_REVOKED) {
            throw new BusinessException(400, "当前分享已撤销");
        }

        shareLink.setStatus(SHARE_STATUS_REVOKED);
        shareLink.setRevokedByUserId(currentAccessContext.getCurrentUserId());
        shareLink.setRevokedAt(LocalDateTime.now());
        shareLink.setUpdatedAt(LocalDateTime.now());
        documentShareLinkMapper.updateById(shareLink);
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(context.knowledgeBase().getTenantId())
            .knowledgeBaseId(context.knowledgeBase().getId())
            .knowledgeBaseName(context.knowledgeBase().getName())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_DOCUMENT_SHARE_LINK)
            .objectId(shareLink.getId())
            .objectTitle(context.document().getTitle())
            .actionType(AuditLogConstants.ACTION_REVOKE_DOCUMENT_SHARE)
            .detail("撤销文档受控分享")
            .build());
    }

    public PublicShareInfoVO getPublicShareInfo(String token) {
        ShareContext context = requirePublicShare(token, false, null);
        PublicShareInfoVO info = new PublicShareInfoVO();
        info.setShareId(context.shareLink().getId());
        info.setDocumentTitle(context.document().getTitle());
        info.setKnowledgeBaseName(context.knowledgeBase().getName());
        info.setAccessCodeRequired(StringUtils.hasText(context.shareLink().getAccessCodeHash()));
        info.setExpiresAt(context.shareLink().getExpiresAt());
        return info;
    }

    public PublicShareDocumentVO accessPublicShare(String token, PublicShareAccessDTO dto) {
        ShareContext context = requirePublicShare(token, true, null);
        String accessCode = dto == null ? null : dto.getAccessCode();
        String normalizedAccessCode = StringUtils.hasText(accessCode) ? accessCode.trim() : null;
        if (StringUtils.hasText(context.shareLink().getAccessCodeHash())
            && !passwordCodec.matches(normalizedAccessCode, context.shareLink().getAccessCodeHash())) {
            auditShareFailure(context, "访问码错误");
            throw new BusinessException(403, "当前分享访问码错误");
        }
        if (StringUtils.hasText(context.shareLink().getAccessCodeHash())
            && passwordCodec.needsRehash(context.shareLink().getAccessCodeHash())) {
            context.shareLink().setAccessCodeHash(passwordCodec.hash(normalizedAccessCode));
        }

        context.shareLink().setLastAccessedAt(LocalDateTime.now());
        context.shareLink().setUpdatedAt(LocalDateTime.now());
        documentShareLinkMapper.updateById(context.shareLink());
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(context.knowledgeBase().getTenantId())
            .knowledgeBaseId(context.knowledgeBase().getId())
            .knowledgeBaseName(context.knowledgeBase().getName())
            .actorType(AuditLogConstants.ACTOR_SHARE_VISITOR)
            .actorDisplayName("外部访客")
            .objectType(AuditLogConstants.OBJECT_DOCUMENT_SHARE_LINK)
            .objectId(context.shareLink().getId())
            .objectTitle(context.document().getTitle())
            .actionType(AuditLogConstants.ACTION_ACCESS_DOCUMENT_SHARE)
            .detail("访问受控分享文档")
            .sourceType(AuditLogConstants.SOURCE_PUBLIC_SHARE)
            .build());
        return convertToPublicDocumentVO(context);
    }

    private ShareContext requireManageableDocument(Long documentId) {
        Document document = requireActiveDocument(documentId);
        if (!"DOC".equals(document.getDocType())) {
            throw new BusinessException(400, "当前节点不是可分享文档");
        }
        KnowledgeBase knowledgeBase = requireActiveKnowledgeBase(document.getKnowledgeBaseId());
        if (!Objects.equals(knowledgeBase.getTenantId(), currentAccessContext.getCurrentTenantId())) {
            throw new BusinessException(403, "无权访问该文档");
        }
        tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);
        return new ShareContext(knowledgeBase, document, null);
    }

    private ShareContext requirePublicShare(String token, boolean auditFailure, String extraDetail) {
        DocumentShareLink shareLink = findShareByToken(token);
        if (shareLink == null) {
            throw new BusinessException(404, "当前分享不存在");
        }

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(shareLink.getKnowledgeBaseId());
        Document document = documentMapper.selectById(shareLink.getDocumentId());
        ShareContext context = new ShareContext(knowledgeBase, document, shareLink);
        if (shareLink.getStatus() == SHARE_STATUS_REVOKED || shareLink.getRevokedAt() != null) {
            if (auditFailure) {
                auditShareFailure(context, "分享已撤销");
            }
            throw new BusinessException(410, "当前分享已撤销");
        }
        if (shareLink.getExpiresAt() != null && shareLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            if (auditFailure) {
                auditShareFailure(context, "分享已过期");
            }
            throw new BusinessException(410, "当前分享已过期");
        }
        if (knowledgeBase == null || knowledgeBase.getStatus() == null || knowledgeBase.getStatus() == 0
            || document == null || document.getStatus() == null || document.getStatus() == 0) {
            if (auditFailure) {
                auditShareFailure(context, StringUtils.hasText(extraDetail) ? extraDetail : "被分享文档不可用");
            }
            throw new BusinessException(410, "被分享文档暂时不可用");
        }
        return context;
    }

    private void auditShareFailure(ShareContext context, String detail) {
        auditLogService.recordFailureSafely(AuditLogCommand.builder()
            .tenantId(context.shareLink().getTenantId())
            .knowledgeBaseId(context.shareLink().getKnowledgeBaseId())
            .knowledgeBaseName(context.knowledgeBase() == null ? null : context.knowledgeBase().getName())
            .actorType(AuditLogConstants.ACTOR_SHARE_VISITOR)
            .actorDisplayName("外部访客")
            .objectType(AuditLogConstants.OBJECT_DOCUMENT_SHARE_LINK)
            .objectId(context.shareLink().getId())
            .objectTitle(context.document() == null ? null : context.document().getTitle())
            .actionType(AuditLogConstants.ACTION_ACCESS_DOCUMENT_SHARE)
            .detail(detail)
            .sourceType(AuditLogConstants.SOURCE_PUBLIC_SHARE)
            .build());
    }

    private DocumentShareLink findShareByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String normalizedToken = token.trim();
        String hashedToken = opaqueTokenCodec.hash(normalizedToken);
        DocumentShareLink shareLink = findShareByStoredToken(hashedToken);
        if (shareLink != null) {
            return shareLink;
        }
        shareLink = findShareByStoredToken(normalizedToken);
        if (shareLink != null && normalizedToken.equals(shareLink.getShareToken())) {
            shareLink.setShareToken(hashedToken);
            shareLink.setSecretHash(hashedToken);
            shareLink.setUpdatedAt(LocalDateTime.now());
            documentShareLinkMapper.updateById(shareLink);
        }
        return shareLink;
    }

    private DocumentShareLink findShareByStoredToken(String storedToken) {
        LambdaQueryWrapper<DocumentShareLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocumentShareLink::getShareToken, storedToken).last("LIMIT 1");
        return documentShareLinkMapper.selectOne(queryWrapper);
    }

    private DocumentShareLink requireTenantShare(Long shareId) {
        DocumentShareLink shareLink = documentShareLinkMapper.selectById(shareId);
        if (shareLink == null) {
            throw new BusinessException(404, "当前分享不存在");
        }
        if (!Objects.equals(shareLink.getTenantId(), currentAccessContext.getCurrentTenantId())) {
            throw new BusinessException(403, "无权访问该分享");
        }
        return shareLink;
    }

    private Document requireActiveDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null || document.getStatus() == null || document.getStatus() == 0) {
            throw new BusinessException(404, "文档不存在");
        }
        return document;
    }

    private KnowledgeBase requireActiveKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || knowledgeBase.getStatus() == null || knowledgeBase.getStatus() == 0) {
            throw new BusinessException(404, "知识库不存在");
        }
        return knowledgeBase;
    }

    private Integer resolveExpiresInDays(Integer expiresInDays) {
        return expiresInDays == null || expiresInDays <= 0 ? defaultExpireDays : expiresInDays;
    }

    private DocumentShareLinkVO convertToVO(DocumentShareLink shareLink, Document document, String rawShareToken) {
        DocumentShareLinkVO vo = new DocumentShareLinkVO();
        vo.setId(shareLink.getId());
        vo.setTenantId(shareLink.getTenantId());
        vo.setKnowledgeBaseId(shareLink.getKnowledgeBaseId());
        vo.setDocumentId(shareLink.getDocumentId());
        vo.setDocumentTitle(document == null ? null : document.getTitle());
        vo.setShareToken(StringUtils.hasText(rawShareToken) ? rawShareToken : null);
        vo.setShareUrl(StringUtils.hasText(rawShareToken) ? "/share/" + rawShareToken : null);
        vo.setStatus(shareLink.getStatus());
        vo.setExpired(shareLink.getExpiresAt() != null && shareLink.getExpiresAt().isBefore(LocalDateTime.now()));
        vo.setAccessCodeProtected(StringUtils.hasText(shareLink.getAccessCodeHash()));
        vo.setExpiresAt(shareLink.getExpiresAt());
        vo.setCreatedByUserId(shareLink.getCreatedByUserId());
        vo.setRevokedByUserId(shareLink.getRevokedByUserId());
        vo.setRevokedAt(shareLink.getRevokedAt());
        vo.setLastAccessedAt(shareLink.getLastAccessedAt());
        vo.setCreatedAt(shareLink.getCreatedAt());
        return vo;
    }

    private PublicShareDocumentVO convertToPublicDocumentVO(ShareContext context) {
        PublicShareDocumentVO vo = new PublicShareDocumentVO();
        vo.setShareId(context.shareLink().getId());
        vo.setDocumentId(context.document().getId());
        vo.setKnowledgeBaseId(context.document().getKnowledgeBaseId());
        vo.setKnowledgeBaseName(context.knowledgeBase().getName());
        vo.setTitle(context.document().getTitle());
        vo.setFormat(context.document().getFormat());
        vo.setContent(context.document().getContent());
        vo.setContentText(context.document().getContentText());
        vo.setSummary(context.document().getSummary());
        vo.setVersionNo(context.document().getVersionNo());
        vo.setUpdatedAt(context.document().getUpdatedAt());
        vo.setExpiresAt(context.shareLink().getExpiresAt());
        return vo;
    }

    private record ShareContext(KnowledgeBase knowledgeBase, Document document, DocumentShareLink shareLink) {
    }
}
