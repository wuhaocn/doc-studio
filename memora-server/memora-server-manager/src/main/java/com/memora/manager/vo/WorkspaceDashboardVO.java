package com.memora.manager.vo;

import lombok.Data;

import java.util.List;

@Data
public class WorkspaceDashboardVO {
    private WorkspaceVO workspace;

    private long knowledgeBaseCount;

    private long documentCount;

    private long publicKnowledgeBaseCount;

    private long syncEnabledKnowledgeBaseCount;

    private long pendingSyncJobCount;

    private List<KnowledgeBaseVO> knowledgeBases;

    private List<DocumentVO> recentDocuments;

    private List<SyncJobVO> recentSyncJobs;

    private List<TenantMemberVO> members;
}
