package com.yicheng.modules.hospital.pojo.entity;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * (Hospital)表实体类
 *
 * @author zyc
 * @since 2026-02-10 14:37:49
 */
@Data
@TableName("hospital")
public class HospitalEntity {
    private Long id;
//医院名称

    private String name;
//详细地址

    private String address;
//经度

    private Double longitude;
//纬度

    private Double latitude;
//医院图片

    private String avatar;
}

