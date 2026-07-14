package com.bdis.modules.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.research.entity.ResearchAchievementEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ResearchAchievementMapper extends BaseMapper<ResearchAchievementEntity> {
    @Select("SELECT * FROM research_achievement WHERE achievement_no = #{achievementNo} LIMIT 1")
    ResearchAchievementEntity selectByAchievementNoIncludingDeleted(
            @Param("achievementNo") String achievementNo);
}
