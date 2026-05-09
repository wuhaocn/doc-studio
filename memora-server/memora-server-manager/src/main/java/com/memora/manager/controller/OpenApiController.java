package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.dto.OpenApiDocumentCreateDTO;
import com.memora.manager.dto.OpenApiDocumentUpdateDTO;
import com.memora.manager.service.OpenApiDocumentService;
import com.memora.manager.vo.DocumentVO;
import com.memora.manager.vo.DocumentVersionVO;
import com.memora.manager.vo.KnowledgeBaseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/documents/{documentId}")
    public Result<DocumentVO> getDocument(@PathVariable Long documentId) {
        return Result.success(openApiDocumentService.getDocument(documentId));
    }

    @GetMapping("/documents/{documentId}/versions")
    public Result<List<DocumentVersionVO>> getVersions(@PathVariable Long documentId) {
        return Result.success(openApiDocumentService.getVersions(documentId));
    }

    @PostMapping("/documents")
    public Result<DocumentVO> createDocument(@Valid @RequestBody OpenApiDocumentCreateDTO dto) {
        return Result.success(openApiDocumentService.createDocument(dto));
    }

    @PutMapping("/documents/{documentId}")
    public Result<DocumentVO> updateDocument(@PathVariable Long documentId, @Valid @RequestBody OpenApiDocumentUpdateDTO dto) {
        return Result.success(openApiDocumentService.updateDocument(documentId, dto));
    }
}
