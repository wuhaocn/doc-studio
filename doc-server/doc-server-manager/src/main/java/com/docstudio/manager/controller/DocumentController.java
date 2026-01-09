package com.docstudio.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.docstudio.common.result.Result;
import com.docstudio.manager.dto.DocumentCreateDTO;
import com.docstudio.manager.dto.DocumentUpdateDTO;
import com.docstudio.manager.service.DocumentService;
import com.docstudio.manager.vo.DocumentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档控制器
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    
    /**
     * 创建文档
     */
    @PostMapping
    public Result<DocumentVO> create(@Valid @RequestBody DocumentCreateDTO dto) {
        // TODO: 从鉴权中获取userId，暂时使用DTO中的userId
        if (dto.getUserId() == null) {
            dto.setUserId(1L); // 临时默认值
        }
        DocumentVO vo = documentService.create(dto);
        return Result.success(vo);
    }
    
    /**
     * 更新文档
     */
    @PutMapping("/{id}")
    public Result<DocumentVO> update(@PathVariable Long id, 
                                    @Valid @RequestBody DocumentUpdateDTO dto) {
        DocumentVO vo = documentService.update(id, dto);
        return Result.success(vo);
    }
    
    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }
    
    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    public Result<DocumentVO> getById(@PathVariable Long id) {
        DocumentVO vo = documentService.getById(id);
        return Result.success(vo);
    }
    
    /**
     * 分页查询文档列表
     */
    @GetMapping
    public Result<IPage<DocumentVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long knowledgeBaseId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Long userId) {
        IPage<DocumentVO> result = documentService.list(page, size, keyword, knowledgeBaseId, parentId, userId);
        return Result.success(result);
    }
    
    /**
     * 获取知识库下的文档列表（不分页）
     */
    @GetMapping("/knowledge-base/{knowledgeBaseId}")
    public Result<List<DocumentVO>> listByKnowledgeBaseId(
            @PathVariable Long knowledgeBaseId,
            @RequestParam(required = false) Long parentId) {
        List<DocumentVO> list = documentService.listByKnowledgeBaseId(knowledgeBaseId, parentId);
        return Result.success(list);
    }
}

