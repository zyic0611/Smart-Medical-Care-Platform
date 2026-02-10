package com.yicheng.modules.hospital.controller;


import com.yicheng.common.Result;
import com.yicheng.modules.hospital.pojo.entity.HospitalEntity;
import com.yicheng.modules.hospital.pojo.vo.HospitalVO;
import com.yicheng.modules.hospital.service.IHospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hospital")
@RequiredArgsConstructor
@Tag(name="医院")
public class HospitalController {
    private final IHospitalService hospitalService;

    @GetMapping("/nearby")
    @Operation(summary = "查找附近医院")
    public Result<List<HospitalVO>> findNearbyHospital(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(defaultValue = "5")Double range
            ){
        List<HospitalVO>list=hospitalService.getNearBy(longitude,latitude,range);
        return Result.success(list);

    }
}
