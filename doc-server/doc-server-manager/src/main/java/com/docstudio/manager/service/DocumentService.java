package com.docstudio.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.docstudio.common.exception.BusinessException;
import com.docstudio.manager.dto.DocumentCreateDTO;
import com.docstudio.manager.dto.DocumentUpdateDTO;
import com.docstudio.manager.entity.Document;
import com.docstudio.manager.entity.KnowledgeBase;
import com.docstudio.manager.mapper.DocumentMapper;
import com.docstudio.manager.mapper.KnowledgeBaseMapper;
import com.docstudio.manager.vo.DocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档服务
 */
@Service
@RequiredArgsConstructor
public class DocumentService extends ServiceImpl<DocumentMapper, Document> {
    
    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    
    /**
     * 创建文档
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO create(DocumentCreateDTO dto) {
        // 验证知识库是否存在
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(dto.getKnowledgeBaseId());
        if (knowledgeBase == null || knowledgeBase.getStatus() == 0) {
            throw new BusinessException(404, "知识库不存在");
        }
        
        Document document = new Document();
        BeanUtils.copyProperties(dto, document);
        document.setStatus(1);
        document.setIsPublic(0);
        document.setViewCount(0);
        document.setSortOrder(0);
        if (document.getParentId() == null) {
            document.setParentId(0L);
        }
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        this.save(document);
        
        // 更新知识库文档数量
        knowledgeBaseMapper.incrementDocumentCount(dto.getKnowledgeBaseId());
        
        return convertToVO(document);
    }
    
    /**
     * 更新文档
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO update(Long id, DocumentUpdateDTO dto) {
        Document document = super.getById(id);
        if (document == null) {
            throw new BusinessException(404, "文档不存在");
        }
        if (document.getStatus() == 0) {
            throw new BusinessException(400, "文档已删除");
        }
        
        if (StringUtils.hasText(dto.getTitle())) {
            document.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            document.setContent(dto.getContent());
        }
        if (dto.getContentText() != null) {
            document.setContentText(dto.getContentText());
        }
        document.setUpdatedAt(LocalDateTime.now());
        
        this.updateById(document);
        
        return convertToVO(document);
    }
    
    /**
     * 删除文档（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Document document = super.getById(id);
        if (document == null) {
            throw new BusinessException(404, "文档不存在");
        }
        
        document.setStatus(0);
        document.setUpdatedAt(LocalDateTime.now());
        this.updateById(document);
        
        // 更新知识库文档数量
        knowledgeBaseMapper.decrementDocumentCount(document.getKnowledgeBaseId());
    }
    
    /**
     * 根据ID获取文档
     */
    public DocumentVO getById(Long id) {
        Document document = super.getById(id);
        if (document == null || document.getStatus() == 0) {
            throw new BusinessException(404, "文档不存在");
        }
        return convertToVO(document);
    }
    
    /**
     * 分页查询文档列表
     */
    public IPage<DocumentVO> list(Integer page, Integer size, String keyword, Long knowledgeBaseId, Long parentId, Long userId) {
        Page<Document> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        
        queryWrapper.eq(Document::getStatus, 1);
        
        if (knowledgeBaseId != null) {
            queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId);
        }
        
        if (parentId != null) {
            queryWrapper.eq(Document::getParentId, parentId);
        }
        
        if (userId != null) {
            queryWrapper.eq(Document::getUserId, userId);
        }
        
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like(Document::getTitle, keyword)
                .or()
                .like(Document::getContentText, keyword)
            );
        }
        
        queryWrapper.orderByDesc(Document::getCreatedAt);
        
        IPage<Document> result = this.page(pageParam, queryWrapper);
        
        IPage<DocumentVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList()));
        
        return voPage;
    }
    
    /**
     * 获取知识库下的文档列表（不分页）
     */
    public List<DocumentVO> listByKnowledgeBaseId(Long knowledgeBaseId, Long parentId) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getStatus, 1);
        
        if (parentId != null) {
            queryWrapper.eq(Document::getParentId, parentId);
        } else {
            queryWrapper.eq(Document::getParentId, 0);
        }
        
        queryWrapper.orderByAsc(Document::getSortOrder)
            .orderByDesc(Document::getCreatedAt);
        
        List<Document> list = this.list(queryWrapper);
        return list.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 转换为VO
     */
    private DocumentVO convertToVO(Document document) {
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(document, vo);
        return vo;
    }
}

