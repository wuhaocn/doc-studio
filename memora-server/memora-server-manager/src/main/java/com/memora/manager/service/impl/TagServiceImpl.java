package com.memora.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memora.common.exception.BusinessException;
import com.memora.manager.dto.TagCreateDTO;
import com.memora.manager.dto.TagUpdateDTO;
import com.memora.manager.entity.ResourceTag;
import com.memora.manager.entity.Tag;
import com.memora.manager.mapper.ResourceTagMapper;
import com.memora.manager.mapper.TagMapper;
import com.memora.manager.service.TagService;
import com.memora.manager.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务实现
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {
    
    private final TagMapper tagMapper;
    private final ResourceTagMapper resourceTagMapper;
    
    /**
     * 创建标签
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public TagVO create(TagCreateDTO dto) {
        // 检查标签名称是否已存在
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tag::getName, dto.getName())
            .eq(Tag::getStatus, 1);
        if (tagMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(400, "标签名称已存在");
        }
        
        Tag tag = new Tag();
        BeanUtils.copyProperties(dto, tag);
        tag.setStatus(1);
        tag.setResourceCount(0);
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());
        
        this.save(tag);
        
        return convertToVO(tag);
    }
    
    /**
     * 更新标签
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public TagVO update(Long id, TagUpdateDTO dto) {
        Tag tag = super.getById(id);
        if (tag == null || tag.getStatus() == 0) {
            throw new BusinessException(404, "标签不存在");
        }
        
        // 检查新名称是否与其他标签重复
        if (!tag.getName().equals(dto.getName())) {
            LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Tag::getName, dto.getName())
                .eq(Tag::getStatus, 1)
                .ne(Tag::getId, id);
            if (tagMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException(400, "标签名称已存在");
            }
        }
        
        BeanUtils.copyProperties(dto, tag);
        tag.setUpdatedAt(LocalDateTime.now());
        this.updateById(tag);
        
        return convertToVO(tag);
    }
    
    /**
     * 删除标签
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        Tag tag = super.getById(id);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        
        tag.setStatus(0);
        tag.setUpdatedAt(LocalDateTime.now());
        this.updateById(tag);
        
        // 删除与资源的关联
        LambdaQueryWrapper<ResourceTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ResourceTag::getTagId, id);
        resourceTagMapper.delete(queryWrapper);
    }
    
    /**
     * 获取标签详情
     */
    @Override
    public TagVO getById(Long id) {
        Tag tag = super.getById(id);
        if (tag == null || tag.getStatus() == 0) {
            throw new BusinessException(404, "标签不存在");
        }
        return convertToVO(tag);
    }
    
    /**
     * 获取标签列表
     */
    @Override
    public List<TagVO> getList() {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tag::getStatus, 1)
            .orderByDesc(Tag::getResourceCount)
            .orderByDesc(Tag::getCreatedAt);
        
        List<Tag> list = this.list(queryWrapper);
        return list.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 搜索标签
     */
    @Override
    public List<TagVO> search(String keyword) {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tag::getStatus, 1);
        
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Tag::getName, keyword);
        }
        
        queryWrapper.orderByDesc(Tag::getResourceCount)
            .orderByDesc(Tag::getCreatedAt);
        
        List<Tag> list = this.list(queryWrapper);
        return list.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取资源的标签
     */
    @Override
    public List<TagVO> getByResourceId(Long resourceId) {
        LambdaQueryWrapper<ResourceTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ResourceTag::getResourceId, resourceId);
        
        List<ResourceTag> resourceTags = resourceTagMapper.selectList(queryWrapper);
        if (resourceTags.isEmpty()) {
            return List.of();
        }
        
        List<Long> tagIds = resourceTags.stream()
            .map(ResourceTag::getTagId)
            .collect(Collectors.toList());
        
        LambdaQueryWrapper<Tag> tagQueryWrapper = new LambdaQueryWrapper<>();
        tagQueryWrapper.in(Tag::getId, tagIds)
            .eq(Tag::getStatus, 1);
        
        List<Tag> tags = tagMapper.selectList(tagQueryWrapper);
        return tags.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 为资源添加标签
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addTagToResource(Long resourceId, Long tagId) {
        // 检查标签是否存在
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null || tag.getStatus() == 0) {
            throw new BusinessException(404, "标签不存在");
        }
        
        // 检查关联是否已存在
        LambdaQueryWrapper<ResourceTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ResourceTag::getResourceId, resourceId)
            .eq(ResourceTag::getTagId, tagId);
        if (resourceTagMapper.selectCount(queryWrapper) == 0) {
            ResourceTag resourceTag = new ResourceTag();
            resourceTag.setResourceId(resourceId);
            resourceTag.setTagId(tagId);
            resourceTag.setCreatedAt(LocalDateTime.now());
            resourceTagMapper.insert(resourceTag);
            
            // 更新标签的资源计数
            tag.setResourceCount(tag.getResourceCount() + 1);
            tagMapper.updateById(tag);
        }
    }
    
    /**
     * 从资源移除标签
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeTagFromResource(Long resourceId, Long tagId) {
        // 检查关联是否存在
        LambdaQueryWrapper<ResourceTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ResourceTag::getResourceId, resourceId)
            .eq(ResourceTag::getTagId, tagId);
        if (resourceTagMapper.selectCount(queryWrapper) > 0) {
            resourceTagMapper.delete(queryWrapper);
            
            // 更新标签的资源计数
            Tag tag = tagMapper.selectById(tagId);
            if (tag != null && tag.getStatus() == 1 && tag.getResourceCount() > 0) {
                tag.setResourceCount(tag.getResourceCount() - 1);
                tagMapper.updateById(tag);
            }
        }
    }
    
    /**
     * 转换为标签VO
     */
    private TagVO convertToVO(Tag tag) {
        TagVO vo = new TagVO();
        BeanUtils.copyProperties(tag, vo);
        return vo;
    }
}