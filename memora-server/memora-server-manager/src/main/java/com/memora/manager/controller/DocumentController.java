package com.memora.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.memora.common.result.Result;
import com.memora.manager.dto.DocumentBatchDeleteDTO;
import com.memora.manager.dto.DocumentBatchMoveDTO;
import com.memora.manager.dto.DocumentCreateDTO;
import com.memora.manager.dto.DocumentPublishUpdateDTO;
import com.memora.manager.dto.DocumentSortDTO;
import com.memora.manager.dto.DocumentUpdateDTO;
import com.memora.manager.entity.DocumentVersion;
import com.memora.manager.service.DocumentService;
import com.memora.manager.vo.DocumentVO;
import com.memora.manager.vo.DocumentVersionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping
    public Result<DocumentVO> create(@Valid @RequestBody DocumentCreateDTO dto) {
        return Result.success(documentService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<DocumentVO> update(@PathVariable Long id, @Valid @RequestBody DocumentUpdateDTO dto) {
        return Result.success(documentService.update(id, dto));
    }

    @PutMapping("/{id}/publish")
    public Result<DocumentVO> publish(@PathVariable Long id, @RequestBody(required = false) DocumentPublishUpdateDTO dto) {
        return Result.success(documentService.publish(id, dto));
    }

    @DeleteMapping("/{id}/publish")
    public Result<DocumentVO> unpublish(@PathVariable Long id) {
        return Result.success(documentService.unpublish(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/restore")
    public Result<DocumentVO> restore(@PathVariable Long id) {
        return Result.success(documentService.restore(id));
    }

    @PostMapping("/batch-move")
    public Result<Void> batchMove(@Valid @RequestBody DocumentBatchMoveDTO dto) {
        documentService.batchMove(dto);
        return Result.success();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@Valid @RequestBody DocumentBatchDeleteDTO dto) {
        documentService.batchDelete(dto);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<DocumentVO> getById(@PathVariable Long id) {
        return Result.success(documentService.getById(id));
    }

    @GetMapping
    public Result<IPage<DocumentVO>> list(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long knowledgeBaseId,
        @RequestParam(required = false) Long parentId,
        @RequestParam(required = false) Long userId) {
        return Result.success(documentService.list(page, size, keyword, knowledgeBaseId, parentId, userId));
    }

    @GetMapping("/trash")
    public Result<List<DocumentVO>> listDeleted(@RequestParam(required = false) Long knowledgeBaseId) {
        return Result.success(documentService.listDeleted(knowledgeBaseId));
    }

    @GetMapping("/knowledge-base/{knowledgeBaseId}")
    public Result<List<DocumentVO>> listByKnowledgeBaseId(
        @PathVariable Long knowledgeBaseId,
        @RequestParam(required = false) Long parentId) {
        return Result.success(documentService.listByKnowledgeBaseId(knowledgeBaseId, parentId));
    }

    @GetMapping("/knowledge-base/{knowledgeBaseId}/tree")
    public Result<List<DocumentVO>> listTreeByKnowledgeBaseId(@PathVariable Long knowledgeBaseId) {
        return Result.success(documentService.listTreeByKnowledgeBaseId(knowledgeBaseId));
    }

    @PutMapping("/sort")
    public Result<Void> updateSortOrder(@RequestBody List<DocumentSortDTO> sortList) {
        return documentService.updateSortOrder(sortList);
    }

    @GetMapping("/{id}/versions")
    public Result<List<DocumentVersionVO>> getVersions(@PathVariable Long id) {
        return Result.success(documentService.getVersions(id));
    }

    @GetMapping("/versions/{versionId}")
    public Result<DocumentVersion> getVersionById(@PathVariable Long versionId) {
        return Result.success(documentService.getVersionById(versionId));
    }

    @PostMapping("/{id}/rollback/{versionId}")
    public Result<DocumentVO> rollbackToVersion(@PathVariable Long id, @PathVariable Long versionId) {
        return Result.success(documentService.rollbackToVersion(id, versionId));
    }
}
