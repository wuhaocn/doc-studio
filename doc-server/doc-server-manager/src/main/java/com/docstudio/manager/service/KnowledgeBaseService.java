package com.docstudio.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.docstudio.common.exception.BusinessException;
import com.docstudio.manager.dto.KnowledgeBaseCreateDTO;
import com.docstudio.manager.dto.KnowledgeBaseUpdateDTO;
import com.docstudio.manager.entity.KnowledgeBase;
import com.docstudio.manager.mapper.KnowledgeBaseMapper;
import com.docstudio.manager.vo.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库服务
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> {
    
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    
    /**
     * 创建知识库
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO create(KnowledgeBaseCreateDTO dto) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(dto, knowledgeBase);
        knowledgeBase.setStatus(1);
        knowledgeBase.setIsPublic(0);
        knowledgeBase.setDocumentCount(0);
        knowledgeBase.setViewCount(0);
        knowledgeBase.setSortOrder(0);
        knowledgeBase.setCreatedAt(LocalDateTime.now());
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        
        this.save(knowledgeBase);
        
        return convertToVO(knowledgeBase);
    }
    
    /**
     * 更新知识库
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO update(Long id, KnowledgeBaseUpdateDTO dto) {
        KnowledgeBase knowledgeBase = super.getById(id);
        if (knowledgeBase == null) {
            throw new BusinessException(404, "知识库不存在");
        }
        if (knowledgeBase.getStatus() == 0) {
            throw new BusinessException(400, "知识库已删除");
        }
        
        if (StringUtils.hasText(dto.getName())) {
            knowledgeBase.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            knowledgeBase.setDescription(dto.getDescription());
        }
        if (dto.getCover() != null) {
            knowledgeBase.setCover(dto.getCover());
        }
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        
        this.updateById(knowledgeBase);
        
        return convertToVO(knowledgeBase);
    }
    
    /**
     * 删除知识库（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        KnowledgeBase knowledgeBase = super.getById(id);
        if (knowledgeBase == null) {
            throw new BusinessException(404, "知识库不存在");
        }
        
        knowledgeBase.setStatus(0);
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        this.updateById(knowledgeBase);
    }
    
    /**
     * 根据ID获取知识库
     */
    public KnowledgeBaseVO getById(Long id) {
        KnowledgeBase knowledgeBase = super.getById(id);
        if (knowledgeBase == null || knowledgeBase.getStatus() == 0) {
            throw new BusinessException(404, "知识库不存在");
        }
        return convertToVO(knowledgeBase);
    }
    
    /**
     * 分页查询知识库列表
     */
    public IPage<KnowledgeBaseVO> list(Integer page, Integer size, String keyword, Long userId) {
        Page<KnowledgeBase> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        
        queryWrapper.eq(KnowledgeBase::getStatus, 1);
        
        if (userId != null) {
            queryWrapper.eq(KnowledgeBase::getUserId, userId);
        }
        
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like(KnowledgeBase::getName, keyword)
                .or()
                .like(KnowledgeBase::getDescription, keyword)
            );
        }
        
        queryWrapper.orderByDesc(KnowledgeBase::getCreatedAt);
        
        IPage<KnowledgeBase> result = this.page(pageParam, queryWrapper);
        
        IPage<KnowledgeBaseVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList()));
        
        return voPage;
    }
    
    /**
     * 获取用户的知识库列表（不分页）
     */
    public List<KnowledgeBaseVO> listByUserId(Long userId) {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getUserId, userId)
            .eq(KnowledgeBase::getStatus, 1)
            .orderByDesc(KnowledgeBase::getCreatedAt);
        
        List<KnowledgeBase> list = this.list(queryWrapper);
        return list.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 转换为VO
     */
    private KnowledgeBaseVO convertToVO(KnowledgeBase knowledgeBase) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, vo);
        return vo;
    }
}

