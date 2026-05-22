package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.service.PublicSiteService;
import com.memora.manager.vo.PublicSiteDocumentVO;
import com.memora.manager.vo.PublicSiteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/sites")
@RequiredArgsConstructor
public class PublicSiteController {
    private final PublicSiteService publicSiteService;

    @GetMapping("/{siteSlug}")
    public Result<PublicSiteVO> getSite(@PathVariable String siteSlug) {
        return Result.success(publicSiteService.getSite(siteSlug));
    }

    @GetMapping("/{siteSlug}/{publicSlug}")
    public Result<PublicSiteDocumentVO> getDocument(@PathVariable String siteSlug, @PathVariable String publicSlug) {
        return Result.success(publicSiteService.getDocument(siteSlug, publicSlug));
    }
}
