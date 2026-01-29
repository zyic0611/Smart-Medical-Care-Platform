package com.yicheng.modules.sysdict.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.modules.sysdict.pojo.entity.SysDict;

import java.util.List;

public interface ISysDictService extends IService<SysDict> {
    /**
     * [核心] 根据字典code获取字典列表 (用于前端下拉框)
     * 例如：输入 "gender" -> 返回 [{"男", "1"}, {"女", "2"}]
     */
    List<SysDict> listByCode(String code);

    /**
     * [核心] 根据字典code和key获取字典值 (用于后端翻译)
     * 例如：输入 "gender", "1" -> 返回 "男"
     */
    String getValue(String code, String dictKey);
}
