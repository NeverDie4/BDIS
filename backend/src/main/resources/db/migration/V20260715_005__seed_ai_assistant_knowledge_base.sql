-- 首批 AI 小助手知识库。文档与切片使用稳定编码，便于后续增量维护。

START TRANSACTION;

SET @doc_code = 'KB-SYSTEM-GUIDE-V1';
SET @doc_title = '系统定位与 AI 小助手回答边界';
SET @doc_type = 'system_guide';
SET @doc_content = '这个系统是干什么的？本系统用于中药材采集任务、批次档案、现场图片、图谱识别、生长观测、审核和溯源管理。批次档案是什么？批次档案汇总同一次采集的图片、识别结论、复核状态和评价信息，用于后续查看、确认和归档。\n\nAI 小助手可以解释页面中的业务概念、操作流程、识别结果、审核状态和溯源规则，但不能编造系统中不存在的数据、页面、接口或操作结果。AI 小助手能做什么？它可以根据知识库说明系统流程，并在用户提供任务 ID、批次 ID、图片 ID 或生长记录 ID 后解释相关数据。信息不足时，应提示用户补充编号或上下文。\n\nAI 小助手不能代替用户修改识别结果、完成人工复核、发布任务、提交审核、确认批次完成、开启公开溯源、归档或删除业务数据。未通过系统工具查询到数据时，应明确说明未查询到，不能自行推断。';
INSERT INTO herb_ai_knowledge_doc (
    doc_code, doc_title, doc_type, source_type, file_name, content_text,
    summary, status, embedding_status, chunk_count, remark, is_deleted
)
SELECT @doc_code, @doc_title, @doc_type, 'markdown',
       '10_AI小助手知识库种子文档_V1.0_全组_20260711.md', @doc_content,
       '说明系统定位、AI 小助手能力范围与禁止代办事项。',
       'enabled', 'pending', 1, 'seed:ai-assistant-knowledge-v1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code
);
SET @doc_id = (SELECT id FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code LIMIT 1);
INSERT INTO herb_ai_knowledge_chunk (
    doc_id, chunk_index, chunk_title, chunk_content, content_hash,
    token_count, embedding_status, metadata_json, is_deleted
)
SELECT @doc_id, 1, CONCAT(@doc_title, ' #1'), @doc_content, SHA2(@doc_content, 256),
       CHAR_LENGTH(@doc_content), 'pending',
       JSON_OBJECT('docId', @doc_id, 'docCode', @doc_code, 'docTitle', @doc_title,
                   'docType', @doc_type, 'chunkIndex', 1), 0
WHERE @doc_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM herb_ai_knowledge_chunk
      WHERE doc_id = @doc_id AND chunk_index = 1 AND is_deleted = 0
  );

SET @doc_code = 'KB-COLLECTION-BATCH-V1';
SET @doc_title = '采集任务与批次提交流程';
SET @doc_type = 'system_guide';
SET @doc_content = '采集任务用于组织现场采集工作。管理员或教师创建任务时，必须从当前数据范围内选择有效采集员，并提交真实的采集员信息。新建任务默认是草稿，只有执行正式发布后，采集员才能在手机端看到任务。\n\n怎么进行中药材采集？标准流程是：管理员或教师创建并发布采集任务；采集员在手机端查看任务并创建或进入批次；拍照或选择图片上传；图片绑定批次并自动进入识别；采集员检查图片和生长记录；最后通过批次统一提交审核。手机端怎么使用？手机端主要用于采集员查看已发布任务、进入批次、拍照上传、查看识别状态和统一提交批次。\n\n批次提交审核是批次及其唯一生长记录的统一入口，不应再单独提交生长记录。提交时后端会在同一事务中校验并同步状态，避免只提交一部分数据。任务发布与生长记录提交审核是两个不同操作：前者让采集员可见任务，后者把已采集的数据送交审核。';
INSERT INTO herb_ai_knowledge_doc (
    doc_code, doc_title, doc_type, source_type, file_name, content_text,
    summary, status, embedding_status, chunk_count, remark, is_deleted
)
SELECT @doc_code, @doc_title, @doc_type, 'markdown',
       'herb-system-guide.md', @doc_content,
       '说明任务创建、发布、采集、批次绑定和统一提交审核流程。',
       'enabled', 'pending', 1, 'seed:ai-assistant-knowledge-v1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code
);
SET @doc_id = (SELECT id FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code LIMIT 1);
INSERT INTO herb_ai_knowledge_chunk (
    doc_id, chunk_index, chunk_title, chunk_content, content_hash,
    token_count, embedding_status, metadata_json, is_deleted
)
SELECT @doc_id, 1, CONCAT(@doc_title, ' #1'), @doc_content, SHA2(@doc_content, 256),
       CHAR_LENGTH(@doc_content), 'pending',
       JSON_OBJECT('docId', @doc_id, 'docCode', @doc_code, 'docTitle', @doc_title,
                   'docType', @doc_type, 'chunkIndex', 1), 0
WHERE @doc_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM herb_ai_knowledge_chunk
      WHERE doc_id = @doc_id AND chunk_index = 1 AND is_deleted = 0
  );

SET @doc_code = 'KB-IMAGE-RECOGNITION-V1';
SET @doc_title = '采集图片与自动识别规则';
SET @doc_type = 'rule';
SET @doc_content = '采集图片应保证主体药材清晰、完整，尽量使用自然光或均匀光照，避免模糊、遮挡、过暗、过曝和复杂背景。应对叶、茎、花、果、根或药用部位进行必要的多角度采集，并准确记录采集地点、时间、生长阶段和健康状态。\n\n怎么上传图片？采集员可以在批次采集流程中拍照或选择图片上传，上传成功后图片绑定到当前批次。怎么识别图片？图片绑定批次后，系统会自动提取特征向量并优先执行本地标准图谱匹配。置信度较低、候选结果接近或结果不一致时，系统可使用大模型识别进行辅助判断。图谱识别和大模型识别是什么关系？本地图谱识别是主流程，大模型识别是低置信度场景下的补充参考，不能自动覆盖本地图谱结果或人工复核结论。\n\n图片质量会直接影响识别可靠性。识别结果异常时，应先检查图片清晰度、主体完整性、采集部位和背景干扰，再决定补拍、重新识别或提交人工复核。';
INSERT INTO herb_ai_knowledge_doc (
    doc_code, doc_title, doc_type, source_type, file_name, content_text,
    summary, status, embedding_status, chunk_count, remark, is_deleted
)
SELECT @doc_code, @doc_title, @doc_type, 'markdown',
       '10_AI小助手知识库种子文档_V1.0_全组_20260711.md', @doc_content,
       '说明采集图片规范、上传后自动识别和低置信度辅助判断。',
       'enabled', 'pending', 1, 'seed:ai-assistant-knowledge-v1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code
);
SET @doc_id = (SELECT id FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code LIMIT 1);
INSERT INTO herb_ai_knowledge_chunk (
    doc_id, chunk_index, chunk_title, chunk_content, content_hash,
    token_count, embedding_status, metadata_json, is_deleted
)
SELECT @doc_id, 1, CONCAT(@doc_title, ' #1'), @doc_content, SHA2(@doc_content, 256),
       CHAR_LENGTH(@doc_content), 'pending',
       JSON_OBJECT('docId', @doc_id, 'docCode', @doc_code, 'docTitle', @doc_title,
                   'docType', @doc_type, 'chunkIndex', 1), 0
WHERE @doc_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM herb_ai_knowledge_chunk
      WHERE doc_id = @doc_id AND chunk_index = 1 AND is_deleted = 0
  );

SET @doc_code = 'KB-CONFIDENCE-REVIEW-V1';
SET @doc_title = '识别置信度与人工复核规则';
SET @doc_type = 'rule';
SET @doc_content = '识别置信度表示系统对图片最终识别结果的可信程度，但不能单独作为最终结论。高置信度结果通常较可靠，仍应结合图片质量和批次一致性检查；中等置信度应查看候选结果并结合采集部位、生长阶段和目标药材判断；低置信度应查看大模型识别结果并进行人工复核。\n\n什么情况下需要人工复核？包括置信度低于阈值、本地图谱候选相似度接近、大模型识别与本地图谱结果不一致、同一批次多张图片结果不一致、图片质量较差、采集部位不规范或审核人员对自动结论存在疑问。\n\n审核通过和驳回有什么区别？审核通过表示记录满足当前审核要求，可以进入后续流程；审核驳回表示数据或证据需要修改补充，应根据审核意见处理后重新提交。AI 小助手只能解释状态和建议，不能替代审核员执行通过或驳回。';
INSERT INTO herb_ai_knowledge_doc (
    doc_code, doc_title, doc_type, source_type, file_name, content_text,
    summary, status, embedding_status, chunk_count, remark, is_deleted
)
SELECT @doc_code, @doc_title, @doc_type, 'markdown',
       '10_AI小助手知识库种子文档_V1.0_全组_20260711.md', @doc_content,
       '说明置信度含义、复核触发条件以及审核通过和驳回的区别。',
       'enabled', 'pending', 1, 'seed:ai-assistant-knowledge-v1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code
);
SET @doc_id = (SELECT id FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code LIMIT 1);
INSERT INTO herb_ai_knowledge_chunk (
    doc_id, chunk_index, chunk_title, chunk_content, content_hash,
    token_count, embedding_status, metadata_json, is_deleted
)
SELECT @doc_id, 1, CONCAT(@doc_title, ' #1'), @doc_content, SHA2(@doc_content, 256),
       CHAR_LENGTH(@doc_content), 'pending',
       JSON_OBJECT('docId', @doc_id, 'docCode', @doc_code, 'docTitle', @doc_title,
                   'docType', @doc_type, 'chunkIndex', 1), 0
WHERE @doc_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM herb_ai_knowledge_chunk
      WHERE doc_id = @doc_id AND chunk_index = 1 AND is_deleted = 0
  );

SET @doc_code = 'KB-GROWTH-OBSERVATION-V1';
SET @doc_title = '生长记录与观测点管理';
SET @doc_type = 'system_guide';
SET @doc_content = '什么是观测点？观测点表示生长记录中用于描述实际观测位置的信息。没有观测点时应提示尚无位置记录；只有一个观测点时可以查看该位置的连续记录；多个观测点可用于比较不同位置的生长趋势。\n\n生长记录和采集批次是什么关系？手机端按批次调用正式生长记录接口保存数据，一个批次的多张现场图片共同作为该批次唯一生长记录的证据，不能把多张图片拆成多条记录。生长记录与 Web 生长管理页面使用同一份后端数据。\n\n生长记录怎么提交审核？采集员应从批次统一提交入口提交批次及其唯一生长记录，不能单独重复提交生长记录。审核员如何处理待审核记录？审核员在生长管理工作区查看指标、现场图片、地图、审核历史和溯源信息，再执行通过或驳回。现场图片证据从哪里来？详情会按批次查询并展示该批次上传的现场图片。';
INSERT INTO herb_ai_knowledge_doc (
    doc_code, doc_title, doc_type, source_type, file_name, content_text,
    summary, status, embedding_status, chunk_count, remark, is_deleted
)
SELECT @doc_code, @doc_title, @doc_type, 'markdown',
       '04_生长采集记录审核链路_V1.0_全组_20260713.md', @doc_content,
       '说明观测点、批次与生长记录关系、统一提交、审核及现场图片来源。',
       'enabled', 'pending', 1, 'seed:ai-assistant-knowledge-v1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code
);
SET @doc_id = (SELECT id FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code LIMIT 1);
INSERT INTO herb_ai_knowledge_chunk (
    doc_id, chunk_index, chunk_title, chunk_content, content_hash,
    token_count, embedding_status, metadata_json, is_deleted
)
SELECT @doc_id, 1, CONCAT(@doc_title, ' #1'), @doc_content, SHA2(@doc_content, 256),
       CHAR_LENGTH(@doc_content), 'pending',
       JSON_OBJECT('docId', @doc_id, 'docCode', @doc_code, 'docTitle', @doc_title,
                   'docType', @doc_type, 'chunkIndex', 1), 0
WHERE @doc_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM herb_ai_knowledge_chunk
      WHERE doc_id = @doc_id AND chunk_index = 1 AND is_deleted = 0
  );

SET @doc_code = 'KB-GROWTH-TRACE-V1';
SET @doc_title = '生长记录溯源与公开规则';
SET @doc_type = 'rule';
SET @doc_content = '溯源时间线怎么看？溯源时间线用于按时间查看生长记录的重要状态变化和公开事件，管理端会把状态显示为中文，并将审核历史与溯源事件分开呈现。内部审核意见、驳回原因、内部事件正文和内部元数据不会出现在匿名公开档案中。\n\n溯源二维码怎么生成？管理端可为生长记录生成溯源码和二维码，二维码内容指向 Web 公开档案页。生成溯源码或二维码不等于开启公开查询；记录必须由有权限的用户显式开启公开状态后，匿名访问才可用。未公开时不应跳转到必然返回无权限的公开页。\n\n公开档案只展示公开接口允许返回的信息，现场图片和二维码通过专用受控公开端点提供，不开放通用私有文件路径。外部扫码时，二维码地址必须使用扫码设备可以访问的 Web 基址，不能默认为 localhost。';
INSERT INTO herb_ai_knowledge_doc (
    doc_code, doc_title, doc_type, source_type, file_name, content_text,
    summary, status, embedding_status, chunk_count, remark, is_deleted
)
SELECT @doc_code, @doc_title, @doc_type, 'markdown',
       '04_生长记录溯源码与二维码_V1.0_全组_20260713.md', @doc_content,
       '说明溯源时间线、二维码生成、显式公开和匿名数据边界。',
       'enabled', 'pending', 1, 'seed:ai-assistant-knowledge-v1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code
);
SET @doc_id = (SELECT id FROM herb_ai_knowledge_doc WHERE doc_code = @doc_code LIMIT 1);
INSERT INTO herb_ai_knowledge_chunk (
    doc_id, chunk_index, chunk_title, chunk_content, content_hash,
    token_count, embedding_status, metadata_json, is_deleted
)
SELECT @doc_id, 1, CONCAT(@doc_title, ' #1'), @doc_content, SHA2(@doc_content, 256),
       CHAR_LENGTH(@doc_content), 'pending',
       JSON_OBJECT('docId', @doc_id, 'docCode', @doc_code, 'docTitle', @doc_title,
                   'docType', @doc_type, 'chunkIndex', 1), 0
WHERE @doc_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM herb_ai_knowledge_chunk
      WHERE doc_id = @doc_id AND chunk_index = 1 AND is_deleted = 0
  );

COMMIT;
