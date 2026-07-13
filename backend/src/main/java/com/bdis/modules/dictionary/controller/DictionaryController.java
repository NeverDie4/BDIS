package com.bdis.modules.dictionary.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.dictionary.dto.DictItemRequest;
import com.bdis.modules.dictionary.dto.DictTypeRequest;
import com.bdis.modules.dictionary.service.DictionaryService;
import com.bdis.modules.dictionary.vo.DictItemVO;
import com.bdis.modules.dictionary.vo.DictTypeVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dictionaries")
@RequirePermission("dictionary:view")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public Result<PageResult<DictTypeVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(dictionaryService.pageTypes(page, size, keyword, status));
    }

    @PostMapping
    @RequirePermission("dictionary:manage")
    public Result<Long> create(@Valid @RequestBody DictTypeRequest request) {
        return Result.success(dictionaryService.createType(request));
    }

    @PutMapping("/{typeId}")
    @RequirePermission("dictionary:manage")
    public Result<Void> update(
            @PathVariable Long typeId, @Valid @RequestBody DictTypeRequest request) {
        dictionaryService.updateType(typeId, request);
        return Result.success();
    }

    @DeleteMapping("/{typeId}")
    @RequirePermission("dictionary:manage")
    public Result<Void> delete(@PathVariable Long typeId) {
        dictionaryService.deleteType(typeId);
        return Result.success();
    }

    @GetMapping("/{typeCode}/items")
    public Result<List<DictItemVO>> items(
            @PathVariable String typeCode, @RequestParam(required = false) Integer status) {
        return Result.success(dictionaryService.items(typeCode, status));
    }

    @PostMapping("/{typeCode}/items")
    @RequirePermission("dictionary:manage")
    public Result<Long> createItem(
            @PathVariable String typeCode, @Valid @RequestBody DictItemRequest request) {
        return Result.success(dictionaryService.createItem(typeCode, request));
    }

    @PutMapping("/{typeCode}/items/{itemId}")
    @RequirePermission("dictionary:manage")
    public Result<Void> updateItem(
            @PathVariable String typeCode,
            @PathVariable Long itemId,
            @Valid @RequestBody DictItemRequest request) {
        dictionaryService.updateItem(typeCode, itemId, request);
        return Result.success();
    }

    @DeleteMapping("/{typeCode}/items/{itemId}")
    @RequirePermission("dictionary:manage")
    public Result<Void> deleteItem(@PathVariable String typeCode, @PathVariable Long itemId) {
        dictionaryService.deleteItem(typeCode, itemId);
        return Result.success();
    }
}
