package com.bdis.modules.notification.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.notification.entity.NotificationEntity;
import com.bdis.modules.notification.service.NotificationService;
import com.bdis.modules.permission.service.AuthorizationService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService service; private final AuthorizationService authorizationService;
    public NotificationController(NotificationService service, AuthorizationService authorizationService) { this.service = service; this.authorizationService = authorizationService; }
    @GetMapping public Result<List<NotificationEntity>> list(@RequestParam(required=false) Integer limit) { authorizationService.requirePermission("sys:notification:list"); return Result.success(service.listMine(limit)); }
    @PostMapping("/{id}/read") public Result<Void> read(@PathVariable @Positive Long id) { authorizationService.requirePermission("sys:notification:read"); service.read(id); return Result.success(); }
    @PostMapping("/read-all") public Result<Void> readAll() { authorizationService.requirePermission("sys:notification:read"); service.readAll(); return Result.success(); }
}
