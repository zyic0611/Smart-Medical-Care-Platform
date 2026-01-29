package com.yicheng.modules.sysdict.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据字典表(SysDict)实体类
 *
 * @author zyc
 * @since 2026-01-29 09:50:57
 */
@Data
public class SysDict {


    private Long id;

/**
     * 父主键
     */
    private Long parentId;
/**
     * 字典类型码 (如 gender)
     */
    private String code;
/**
     * 字典值 (如 1)
     */
    private String dictKey;
/**
     * 字典名称 (如 男)
     */
    private String dictValue;
/**
     * 排序
     */
    private Integer sort;


    @TableLogic
    private Integer isDeleted;



}

