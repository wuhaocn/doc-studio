package com.memora.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.memora.manager.dto.TagCreateDTO;
import com.memora.manager.dto.TagUpdateDTO;
import com.memora.manager.entity.Tag;
import com.memora.manager.vo.TagVO;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService extends IService<Tag> {
    /**
     * 创建标签
     */
    TagVO create(TagCreateDTO dto);
    
    /**
     * 更新标签
     */
    TagVO update(Long id, TagUpdateDTO dto);
    
    /**
     * 删除标签
     */
    void delete(Long id);
    
    /**
     * 获取标签详情
     */
    TagVO getById(Long id);
    
    /**
     * 获取标签列表
     */
    List<TagVO> getList();
    
    /**
     * 搜索标签
     */
    List<TagVO> search(String keyword);
    
    /**
     * 获取资源的标签
     */
    List<TagVO> getByResourceId(Long resourceId);
    
    /**
     * 为资源添加标签
     */
    void addTagToResource(Long resourceId, Long tagId);
    
    /**
     * 从资源移除标签
     */
    void removeTagFromResource(Long resourceId, Long tagId);
}