package com.yicheng.service;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yicheng.entity.Elderly;
import com.yicheng.exception.CustomException;
import com.yicheng.mapper.BedMapper;
import com.yicheng.mapper.ElderlyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ElderlyService {
    @Resource
    private ElderlyMapper elderlyMapper;

    @Resource
    private BedMapper bedMapper;

    /**
     * 分页查询老人列表
     * (这里会触发 XML 里的级联查询)
     */
    public PageInfo<Elderly> selectPage(Integer pageNum, Integer pageSize, String name) {
        // 1. 开启分页
        PageHelper.startPage(pageNum, pageSize);

        // 2. 调用 Mapper (这一步会执行 XML 里的 Left Join SQL)
        List<Elderly> list = elderlyMapper.selectAll(name);

        // 3. 包装结果
        return new PageInfo<>(list);
    }


    @Transactional(rollbackFor = Exception.class)
    public void add(Elderly elderly){

        //1 基本参数校验
        // 防止前端传个空对象过
        if (elderly.getName() == null || "".equals(elderly.getName())) {
            throw new CustomException("400", "姓名不能为空");
        }

        // 【处理入院时间】
        // 如果前端选了时间，就用前端的；如果没选(null)，就用当前时间
        if (elderly.getCreateTime() == null) {
            elderly.setCreateTime(LocalDate.now());
        }


        elderlyMapper.insert(elderly);

        //把选中的床位状态改为 1 (占用)
        if (elderly.getBedId() != null) {
            bedMapper.updateStatus(elderly.getBedId(), 1);
        }


    }


    /**
     * 删除老人 (退住)
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
        // 1. 先查出这个老人的实体
        Elderly elderly = elderlyMapper.selectById(id); // 记得在 Mapper 加这个方法

        // 2. 删除老人
        elderlyMapper.deleteById(id);

        // 3.核心业务：把他的床位释放，状态改为 0 (空闲)
        if (elderly != null && elderly.getBedId() != null) {
            bedMapper.updateStatus(elderly.getBedId(), 0);
        }
    }




    //通过ID更新的操作
    public void  updateById(Elderly elderly){
        //先根据ID不变 拿出旧的老人实体
        Elderly oldelderly = elderlyMapper.selectById(elderly.getId());

        //拿到旧的床位
        Integer oldBedId=oldelderly.getBedId();

        //拿到新床位
        Integer newBedId = elderly.getBedId();

        // 2. 判断床位是否发生了变化
        // (如果不相等，说明换房了)
        if (newBedId != null && !newBedId.equals(oldBedId)) {

            // A. 占领新床位
            bedMapper.updateStatus(newBedId, 1);

            // B. 释放旧床位 (如果旧床位存在)
            if (oldBedId != null) {
                bedMapper.updateStatus(oldBedId, 0);
            }
        }
        // 特殊情况：如果用户把床位清空了 (newBedId == null)，也要释放旧床位
        else if (newBedId == null && oldBedId != null) {
            bedMapper.updateStatus(oldBedId, 0);
        }


        //再去更新老人表
        elderlyMapper.updateById(elderly);
    }
}
