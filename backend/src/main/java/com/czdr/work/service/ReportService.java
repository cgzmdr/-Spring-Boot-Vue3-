package com.czdr.work.service;

import com.czdr.work.model.request.DiscussionReportRequest;
import com.czdr.work.model.resource.DiscussionReportResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * 举报（用户侧提交与回执）
 *
 * @author cz
 */
public interface ReportService {

    /** 提交举报（同一内容不可重复举报；限流保护） */
    void report(UUID userId, DiscussionReportRequest request);

    /** 我的举报（含处理进度） */
    EasyPageResult<DiscussionReportResource> myReports(UUID userId, Pageable pageable);
}
