package com.yicheng.modules.medicalimg.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;


@Data
public class MedicalImaging {
    private Integer id;
    private Integer elderId;
    private String fileName;
    private String fileUrl;
    private String fileSize;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
