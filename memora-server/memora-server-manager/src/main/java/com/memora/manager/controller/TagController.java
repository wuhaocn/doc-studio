package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.dto.TagCreateDTO;
import com.memora.manager.dto.TagUpdateDTO;
import com.memora.manager.service.TagService;
import com.memora.manager.vo.TagVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器
 */
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {
    
    private final TagService tagService;
    
    /**
     * 创建标签
     */
    @PostMapping
    public Result<TagVO> create(@Valid @RequestBody TagCreateDTO dto) {
        // TODO: 从鉴权中获取userId，暂时使用DTO中的userId
        if (dto.getUserId() == null) {
            dto.setUserId(1L); // 临时默认值
        }
        TagVO result = tagService.create(dto);
        return Result.success(result);
    }
    
    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    public Result<TagVO> update(@PathVariable Long id, 
                               @Valid @RequestBody TagUpdateDTO dto) {
        TagVO result = tagService.update(id, dto);
        return Result.success(result);
    }
    
    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.success();
    }
    
    /**
     * 获取标签详情
     */
    @GetMapping("/{id}")
    public Result<TagVO> getById(@PathVariable Long id) {
        TagVO result = tagService.getById(id);
        return Result.success(result);
    }
    
    /**
     * 获取标签列表
     */
    @GetMapping
    public Result<List<TagVO>> list(@RequestParam(required = false) String keyword) {
        List<TagVO> result;
        if (keyword != null && !keyword.isEmpty()) {
            result = tagService.search(keyword);
        } else {
            result = tagService.getList();
        }
        return Result.success(result);
    }
    
    /**
     * 获取资源的标签
     */
    @GetMapping("/resource/{resourceId}")
    public Result<List<TagVO>> getByResourceId(@PathVariable Long resourceId) {
        List<TagVO> result = tagService.getByResourceId(resourceId);
        return Result.success(result);
    }
    
    /**
     * 为资源添加标签
     */
    @PostMapping("/resource/{resourceId}/add/{tagId}")
    public Result<Void> addTagToResource(@PathVariable Long resourceId, 
                                        @PathVariable Long tagId) {
        tagService.addTagToResource(resourceId, tagId);
        return Result.success();
    }
    
    /**
     * 从资源移除标签
     */
    @DeleteMapping("/resource/{resourceId}/remove/{tagId}")
    public Result<Void> removeTagFromResource(@PathVariable Long resourceId, 
                                           @PathVariable Long tagId) {
        tagService.removeTagFromResource(resourceId, tagId);
        return Result.success();
    }
}