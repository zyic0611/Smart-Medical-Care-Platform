package com.yicheng.modules.bed.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.modules.bed.entity.Bed;
import com.yicheng.exception.CustomException;
import com.yicheng.modules.bed.mapper.BedMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BedServiceIml extends ServiceImpl<BedMapper, Bed> implements BedService {

    @Resource
    private BedMapper bedMapper;

    @Override
    public IPage<Bed> selectPage(Integer pageNum, Integer pageSize, String bedNumber){
        // 1. 创建 MyBatis-Plus 的分页对象
        Page<Bed> page = new Page<>(pageNum, pageSize);

        // 2. 调用 Mapper (自动分页)
        // 这里的 selectPage 是我们在 Mapper 接口里定义的那个
        return bedMapper.selectPage(page, bedNumber);
    }

    @Override
    public List<Bed> selectFreeBeds() {
        return bedMapper.selectFreeBeds();
    }


    /**
     * 新增床位
     */
    @Override
    public void add(Bed bed) {
        // 1. 查重：床号不能重复
        Bed dbBed = bedMapper.selectByNumber(bed.getBedNumber());
        if (dbBed != null) {
            throw new CustomException("400", "床位号 " + bed.getBedNumber() + " 已存在");
        }
        // 2. 默认状态：空闲 (0)
        if (bed.getStatus() == null) {
            bed.setStatus(0);
        }
        bedMapper.insert(bed);
    }

    /**
     * 修改床位
     */
    @Override
    public void update(Bed bed) {
        bedMapper.updateById(bed);
    }

    /**
     * 删除床位
     */
    @Override
    public void delete(Integer id) {
        Bed dbBed = bedMapper.selectById(id);

        // 1.安全检查 万一 ID 不存在，要拦住
        if (dbBed == null) {
            throw new CustomException("404", "未找到该床位");
        }

        // 2.业务检查 删除的床不能正在被使用 （前端的代码逻辑中 其实只要这个床正在使用 就按不了删除键）
        if (dbBed.getStatus() == 1) {
            throw new CustomException("400", "床位号 " + dbBed.getBedNumber() + " 正在被使用，无法删除");
        }
        bedMapper.deleteById(id);
    }


}
