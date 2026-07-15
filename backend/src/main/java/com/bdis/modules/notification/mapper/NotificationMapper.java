package com.bdis.modules.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.notification.entity.NotificationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface NotificationMapper extends BaseMapper<NotificationEntity> {
    @Select("SELECT * FROM sys_notification WHERE recipient_id=#{userId} ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<NotificationEntity> selectMine(@Param("userId") Long userId, @Param("limit") int limit);
    @Update("UPDATE sys_notification SET read_status=1, read_at=NOW(), updated_at=NOW(), version=version+1 WHERE id=#{id} AND recipient_id=#{userId} AND read_status=0")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);
    @Update("UPDATE sys_notification SET read_status=1, read_at=NOW(), updated_at=NOW(), version=version+1 WHERE recipient_id=#{userId} AND read_status=0")
    int markAllRead(@Param("userId") Long userId);
}
