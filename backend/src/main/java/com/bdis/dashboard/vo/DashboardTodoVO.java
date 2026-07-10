package com.bdis.dashboard.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DashboardTodoVO {

    private String todoType;
    private Long bizId;
    private String title;
    private String status;
    private LocalDateTime submittedAt;
    private String route;
}
