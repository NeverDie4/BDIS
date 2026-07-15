package com.bdis.modules.growth.service;

import com.bdis.common.core.PageResult;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.growth.dto.GrowthAuditCommentRequest;
import com.bdis.modules.growth.dto.GrowthAuditRequest;
import com.bdis.modules.growth.dto.GrowthRecordCreateRequest;
import com.bdis.modules.growth.dto.GrowthRecordUpsertRequest;
import com.bdis.modules.growth.query.GrowthRecordQuery;
import com.bdis.modules.growth.vo.GrowthAuditHistoryVO;
import com.bdis.modules.growth.vo.GrowthChartPointVO;
import com.bdis.modules.growth.vo.GrowthPublicTraceArchiveVO;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.GrowthTraceEventVO;
import com.bdis.modules.growth.vo.GrowthTraceQrCodeVO;
import com.bdis.modules.map.vo.MapPointCollectionSummaryVO;
import java.util.List;

public interface GrowthRecordService {

    List<GrowthRecordVO> listByPointId(Long pointId);

    List<MapPointCollectionSummaryVO> listMapPointSummaries(List<Long> pointIds);

    GrowthRecordVO createForPoint(Long pointId, GrowthRecordCreateRequest request);

    PageResult<GrowthRecordVO> page(GrowthRecordQuery query);

    GrowthRecordVO detail(Long id);

    GrowthRecordVO create(GrowthRecordUpsertRequest request);

    GrowthRecordVO getByBatchId(Long batchId);

    GrowthRecordVO createForBatch(Long batchId, GrowthRecordUpsertRequest request);

    GrowthRecordVO updateForBatch(Long batchId, Long recordId, GrowthRecordUpsertRequest request);

    List<GrowthChartPointVO> getChartByTaskId(Long taskId, String metric);

    GrowthRecordVO importFromSoap(
            String externalNo, GrowthRecordUpsertRequest request, String externalCollectorName);

    GrowthRecordVO update(Long id, GrowthRecordUpsertRequest request);

    void delete(Long id);

    GrowthRecordVO submit(Long id);

    GrowthRecordVO approve(Long id, GrowthAuditCommentRequest request);

    GrowthRecordVO reject(Long id, GrowthAuditCommentRequest request);

    GrowthRecordVO audit(Long id, GrowthAuditRequest request);

    GrowthRecordVO archive(Long id, GrowthAuditCommentRequest request);

    GrowthRecordVO archive(Long id);

    PageResult<GrowthRecordVO> reviewPage(GrowthRecordQuery query);

    List<GrowthAuditHistoryVO> auditHistory(Long id);

    List<GrowthTraceEventVO> trace(Long id);

    GrowthTraceQrCodeVO generateTraceCode(Long id);

    GrowthTraceQrCodeVO generateTraceQrCode(Long id);

    GrowthTraceQrCodeVO getTraceQrCode(Long id);

    GrowthTraceQrCodeVO enablePublicTrace(Long id);

    GrowthTraceQrCodeVO disablePublicTrace(Long id);

    GrowthPublicTraceArchiveVO publicTrace(String traceCode);

    FileContentVO traceQrCodeContent(Long id);

    FileContentVO publicTraceQrCode(String traceCode);

    FileContentVO publicTraceImage(String traceCode, Long imageId);
}
