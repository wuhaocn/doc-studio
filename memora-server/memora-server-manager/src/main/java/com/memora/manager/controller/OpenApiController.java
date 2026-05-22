package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.dto.OpenApiDocumentBatchUpsertDTO;
import com.memora.manager.dto.OpenApiDocumentCreateDTO;
import com.memora.manager.dto.OpenApiDocumentUpsertDTO;
import com.memora.manager.dto.OpenApiDocumentUpdateDTO;
import com.memora.manager.service.OpenApiDocumentService;
import com.memora.manager.vo.OpenApiDocumentConsumeVO;
import com.memora.manager.vo.DocumentVO;
import com.memora.manager.vo.DocumentVersionVO;
import com.memora.manager.vo.KnowledgeBaseVO;
import com.memora.manager.vo.OpenApiDocumentBatchUpsertVO;
import com.memora.manager.vo.OpenApiDocumentUpsertResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/open")
@RequiredArgsConstructor
public class OpenApiController {
    private final OpenApiDocumentService openApiDocumentService;

    @GetMapping("/knowledge-bases")
    public Result<List<KnowledgeBaseVO>> listKnowledgeBases() {
        return Result.success(openApiDocumentService.listKnowledgeBases());
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    public Result<List<DocumentVO>> listDocuments(
        @PathVariable Long knowledgeBaseId,
        @RequestParam(required = false) Long parentId) {
        return Result.success(openApiDocumentService.listDocuments(knowledgeBaseId, parentId));
    }

    @GetMapping("/documents/search")
    public Result<List<DocumentVO>> searchDocuments(
        @RequestParam String keyword,
        @RequestParam(required = false) Long knowledgeBaseId,
        @RequestParam(defaultValue = "20") Integer size
    ) {
        return Result.success(openApiDocumentService.searchDocuments(keyword, knowledgeBaseId, size));
    }

    @GetMapping("/documents/{documentId}")
    public Result<DocumentVO> getDocument(@PathVariable Long documentId) {
        return Result.success(openApiDocumentService.getDocument(documentId));
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents/by-source")
    public Result<OpenApiDocumentConsumeVO> getDocumentBySource(
        @PathVariable Long knowledgeBaseId,
        @RequestParam String sourceExternalId,
        @RequestParam(defaultValue = "metadata") String view
    ) {
        return Result.success(openApiDocumentService.getDocumentBySource(knowledgeBaseId, sourceExternalId, view));
    }

    @GetMapping("/documents/{documentId}/versions")
    public Result<List<DocumentVersionVO>> getVersions(@PathVariable Long documentId) {
        return Result.success(openApiDocumentService.getVersions(documentId));
    }

    @GetMapping("/documents/{documentId}/consume")
    public Result<OpenApiDocumentConsumeVO> consumeDocument(
        @PathVariable Long documentId,
        @RequestParam(defaultValue = "rendered") String view
    ) {
        return Result.success(openApiDocumentService.consumeDocument(documentId, view));
    }

    @PostMapping("/documents")
    public Result<DocumentVO> createDocument(@Valid @RequestBody OpenApiDocumentCreateDTO dto) {
        return Result.success(openApiDocumentService.createDocument(dto));
    }

    @PutMapping("/documents/{documentId}")
    public Result<DocumentVO> updateDocument(@PathVariable Long documentId, @Valid @RequestBody OpenApiDocumentUpdateDTO dto) {
        return Result.success(openApiDocumentService.updateDocument(documentId, dto));
    }

    @PostMapping("/documents/upsert")
    public Result<OpenApiDocumentUpsertResultVO> upsertDocument(@Valid @RequestBody OpenApiDocumentUpsertDTO dto) {
        return Result.success(openApiDocumentService.upsertDocument(dto));
    }

    @PostMapping("/documents/batch-upsert")
    public Result<OpenApiDocumentBatchUpsertVO> batchUpsertDocuments(@Valid @RequestBody OpenApiDocumentBatchUpsertDTO dto) {
        return Result.success(openApiDocumentService.batchUpsertDocuments(dto));
    }

    @DeleteMapping("/documents/{documentId}")
    public Result<Void> deleteDocument(@PathVariable Long documentId) {
        openApiDocumentService.deleteDocument(documentId);
        return Result.success();
    }

    @PostMapping("/documents/{documentId}/restore")
    public Result<DocumentVO> restoreDocument(@PathVariable Long documentId) {
        return Result.success(openApiDocumentService.restoreDocument(documentId));
    }

    @PostMapping("/documents/{documentId}/rollback/{versionId}")
    public Result<DocumentVO> rollbackDocument(@PathVariable Long documentId, @PathVariable Long versionId) {
        return Result.success(openApiDocumentService.rollbackDocument(documentId, versionId));
    }
}
