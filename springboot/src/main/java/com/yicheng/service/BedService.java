package com.yicheng.service;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yicheng.entity.Bed;
import com.yicheng.exception.CustomException;
import com.yicheng.mapper.BedMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BedService {

    @Resource
    private BedMapper bedMapper;

    public PageInfo<Bed> selectPage(Integer pageNum, Integer pageSize, String bedNumber){
        PageHelper.startPage(pageNum, pageSize);
        List<Bed> list = bedMapper.selectPage(bedNumber);
        return new PageInfo<>(list);
    }

    public List<Bed> selectFreeBeds() {
        return bedMapper.selectFreeBeds();
    }


    /**
     * 新增床位
     */
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
    public void update(Bed bed) {
        bedMapper.updateById(bed);
    }

    /**
     * 删除床位
     */
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
