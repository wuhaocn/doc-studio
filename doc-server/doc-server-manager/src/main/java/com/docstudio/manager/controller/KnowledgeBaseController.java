package com.docstudio.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.docstudio.common.result.Result;
import com.docstudio.manager.dto.KnowledgeBaseCreateDTO;
import com.docstudio.manager.dto.KnowledgeBaseUpdateDTO;
import com.docstudio.manager.service.DocumentService;
import com.docstudio.manager.service.KnowledgeBaseService;
import com.docstudio.manager.vo.DocumentVO;
import com.docstudio.manager.vo.KnowledgeBaseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    
    /**
     * 创建知识库
     */
    @PostMapping
    public Result<KnowledgeBaseVO> create(@Valid @RequestBody KnowledgeBaseCreateDTO dto) {
        // TODO: 从鉴权中获取userId，暂时使用DTO中的userId
        if (dto.getUserId() == null) {
            dto.setUserId(1L); // 临时默认值
        }
        KnowledgeBaseVO vo = knowledgeBaseService.create(dto);
        return Result.success(vo);
    }
    
    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    public Result<KnowledgeBaseVO> update(@PathVariable Long id, 
                                          @Valid @RequestBody KnowledgeBaseUpdateDTO dto) {
        KnowledgeBaseVO vo = knowledgeBaseService.update(id, dto);
        return Result.success(vo);
    }
    
    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return Result.success();
    }
    
    /**
     * 获取知识库详情
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBaseVO> getById(@PathVariable Long id) {
        KnowledgeBaseVO vo = knowledgeBaseService.getById(id);
        return Result.success(vo);
    }
    
    /**
     * 分页查询知识库列表
     */
    @GetMapping
    public Result<IPage<KnowledgeBaseVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId) {
        IPage<KnowledgeBaseVO> result = knowledgeBaseService.list(page, size, keyword, userId);
        return Result.success(result);
    }
    
    /**
     * 获取用户的知识库列表（不分页）
     */
    @GetMapping("/user/{userId}")
    public Result<List<KnowledgeBaseVO>> listByUserId(@PathVariable Long userId) {
        List<KnowledgeBaseVO> list = knowledgeBaseService.listByUserId(userId);
        return Result.success(list);
    }
    
    /**
     * 获取知识库下的文档列表
     */
    @GetMapping("/{id}/documents")
    public Result<?> getDocuments(
            @PathVariable Long id,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword) {
        if (page != null && size != null) {
            // 分页查询
            IPage<DocumentVO> result = documentService.list(
                page, size, keyword, id, parentId, null);
            return Result.success(result);
        } else {
            // 不分页查询
            List<DocumentVO> list = documentService.listByKnowledgeBaseId(id, parentId);
            return Result.success(list);
        }
    }
}

