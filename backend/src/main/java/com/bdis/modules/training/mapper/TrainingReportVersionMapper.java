package com.bdis.modules.training.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.training.entity.TrainingReportVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
@org.apache.ibatis.annotations.Mapper
public interface TrainingReportVersionMapper extends BaseMapper<TrainingReportVersionEntity> {
 @Select("SELECT * FROM edu_training_record_report_version WHERE training_record_id=#{recordId} ORDER BY version_no DESC") List<TrainingReportVersionEntity> selectByRecordId(@Param("recordId") Long recordId);
}
