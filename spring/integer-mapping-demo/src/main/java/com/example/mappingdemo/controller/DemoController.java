package com.example.mappingdemo.controller;

import com.example.mappingdemo.dto.ApiResponse;
import com.example.mappingdemo.dto.DemoDto;
import com.example.mappingdemo.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoService demoService;

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<List<DemoDto>>> getRecords() {
        List<DemoDto> records = demoService.getAllRecords();
        return ResponseEntity.ok(ApiResponse.success(records, "Fetched all records successfully"));
    }

    @PostMapping("/standard")
    public ResponseEntity<ApiResponse<DemoDto>> saveStandard(
            @RequestParam("intCol") Integer intCol,
            @RequestParam("bigintCol") Long bigintCol) {
        DemoDto saved = demoService.saveStandard(intCol, bigintCol);
        return ResponseEntity.ok(ApiResponse.success(saved, "Successfully inserted standard values"));
    }

    @PostMapping("/java-wrap-around")
    public ResponseEntity<ApiResponse<DemoDto>> saveJavaWrapAround(
            @RequestParam("value") Long value) {
        DemoDto saved = demoService.saveJavaWrapAround(value);
        return ResponseEntity.ok(ApiResponse.success(saved, "Successfully saved Java-casted wrapped-around value"));
    }

    @PostMapping("/db-int-overflow")
    public ResponseEntity<ApiResponse<Void>> saveDbIntOverflow(
            @RequestParam("value") Long value) {
        demoService.saveDbIntOverflow(value);
        return ResponseEntity.ok(ApiResponse.success(null, "Successfully inserted value into INT column"));
    }

    @PostMapping("/db-bigint-overflow")
    public ResponseEntity<ApiResponse<Void>> saveDbBigintOverflow() {
        demoService.saveDbBigintOverflow();
        return ResponseEntity.ok(ApiResponse.success(null, "Successfully inserted value into BIGINT column"));
    }
}
