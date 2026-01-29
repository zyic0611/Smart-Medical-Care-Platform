package com.yicheng.modules.sysdict.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.common.RedisConstants;
import com.yicheng.modules.sysdict.mapper.SysDictMapper;
import com.yicheng.modules.sysdict.pojo.entity.SysDict;
import com.yicheng.utils.CacheClient;
import com.yicheng.utils.Func;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements ISysDictService {

    private final CacheClient cacheClient;

    @Override
    public List<SysDict> listByCode(String code){
        return cacheClient.queryListWithPassThrough(
                RedisConstants.SYSDICT_CACHE_KEY,
                code,
                SysDict.class,
                this::listByCodeFromDb,
                30L,
                TimeUnit.SECONDS
        );
    }

    /**
     * 数据库回查逻辑
     * 专门提供给 CacheClient 当缓存未命中时调用
     */
    public List<SysDict> listByCodeFromDb(String code) {//传入的方法是根据code查询出一个list
        //先查询父节点 code==code parent_id==0 is_deleted==0
        SysDict parent=this.getOne(new LambdaUpdateWrapper<SysDict>()
                .eq(SysDict::getCode,code)
                .eq(SysDict::getParentId,0L)
                .eq(SysDict::getIsDeleted,0));

        //如果父节点不存在 则传的code有问题 直接返回null 给redis缓存空值
        if(parent==null){
            return null;
        }

        //父节点存在 查询子节点
        //父节点=parent_id 按照sort升序排序 未删除
        return this.list(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getIsDeleted,parent.getId())
                .eq(SysDict::getIsDeleted,0)
                .orderByAsc(SysDict::getSort));


    }

    /**
     * [核心] 根据字典code和key获取字典值 (用于后端翻译)
     * 例如：输入 "gender", "1" -> 返回 "男"
     */
    public String getValue(String code, String dictKey){
        // 调用上面list方法 查redis
        List<SysDict> list = this.listByCode(code);

        if (Func.isEmpty(list)) {
            return "";
        }

        // 使用 Stream 流在内存中过滤，性能极快
        return list.stream()
                .filter(item -> item.getDictKey().equals(dictKey))
                .findFirst()
                .map(SysDict::getDictValue)
                .orElse(""); // 找不到就返回空字符串
    }

}
