package com.yicheng.modules.elder.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.modules.bed.entity.Bed;
import com.yicheng.modules.elder.entity.Elderly;
import com.yicheng.exception.CustomException;
import com.yicheng.modules.bed.mapper.BedMapper;
import com.yicheng.modules.elder.mapper.ElderlyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class ElderlyServiceIml extends ServiceImpl<ElderlyMapper, Elderly> implements IService<Elderly>, ElderlyService {

    @Resource
    private ElderlyMapper elderlyMapper;

    @Resource
    private BedMapper bedMapper;

    /**
     * 1. 分页查询
     * 说明：这里的查询依然依赖 XML 里的 selectAll，因为你已经修复了 XML (加了 columnPrefix)，
     * 所以读操作是安全的。
     */
    @Override
    public IPage<Elderly> selectPage(Integer pageNum, Integer pageSize, String name) {
        Page<Elderly> page = new Page<>(pageNum, pageSize);
        return elderlyMapper.selectAll(page, name);
    }

    /**
     * 2. 新增老人 (带床位联动)
     * 说明：整合了你之前的 add 和 addElderlyWithBed 方法
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addElderlyWithBed(Elderly elderly) {
        // --- A. 校验逻辑 ---
        if (elderly.getName() == null || elderly.getName().trim().isEmpty()) {
            throw new CustomException("400", "姓名不能为空");
        }
        // 默认填写入住时间
        if (elderly.getCreateTime() == null) {
            elderly.setCreateTime(LocalDate.now());
        }

        // --- B. 保存老人 ---
        // 使用 MP 自带的 save，它只处理数据库存在的字段
        this.save(elderly);

        // --- C. 锁定床位 ---
        if (elderly.getBedId() != null) {
            // 使用 Lambda 更新床位状态，防止 BedMapper 也有 XML 问题
            bedMapper.update(null, new LambdaUpdateWrapper<Bed>()
                    .eq(Bed::getId, elderly.getBedId())
                    .set(Bed::getStatus, 1)); // 1-占用
        }
    }

    /**
     * 3. 更新老人 (带换房逻辑) - 【核心修复点】
     * 说明：使用 LambdaUpdateWrapper 彻底绕开 XML ResultMap
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateElderlyWithBed(Elderly elderly) {
        // --- A. 处理床位变更逻辑 ---
        Elderly oldData = getById(elderly.getId());
        // 防空判断，防止查不到数据报错
        if (oldData != null) {
            Integer oldBedId = oldData.getBedId();
            Integer newBedId = elderly.getBedId();

            // 如果床位发生了变化
            if (!Objects.equals(oldBedId, newBedId)) {
                // 1. 释放旧床位
                if (oldBedId != null) {
                    bedMapper.update(null, new LambdaUpdateWrapper<Bed>()
                            .eq(Bed::getId, oldBedId)
                            .set(Bed::getStatus, 0)); // 0-空闲
                }
                // 2. 锁定新床位
                if (newBedId != null) {
                    bedMapper.update(null, new LambdaUpdateWrapper<Bed>()
                            .eq(Bed::getId, newBedId)
                            .set(Bed::getStatus, 1)); // 1-占用
                }
            }
        }

        // --- B. 更新老人信息 (物理免疫 XML 报错写法) ---
        // 我们不把 elderly 实体传给 update，而是自己构建一个 Wrapper
        // 这样 MyBatis 就绝对不会去 XML 里找 bedNumber 了！

        LambdaUpdateWrapper<Elderly> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(Elderly::getId, elderly.getId()) // 指定更新哪个 ID
                .set(Elderly::getName, elderly.getName())
                .set(Elderly::getGender, elderly.getGender())
                .set(Elderly::getAge, elderly.getAge())
                .set(Elderly::getHealthStatus, elderly.getHealthStatus())
                .set(Elderly::getNurseId, elderly.getNurseId())
                .set(Elderly::getBedId, elderly.getBedId());

        // 如果允许修改入住时间，取消下面注释
        // .set(Elderly::getCreateTime, elderly.getCreateTime());

        // 执行更新：第一个参数传 null 是关键！
        this.update(null, updateWrapper);
    }

    /**
     * 4. 删除老人 (退住)
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(Integer id) {
        // 1. 先查出来，为了获取他占用的床位 ID
        Elderly elderly = this.getById(id);

        if (elderly != null) {
            // 2. 释放床位
            if (elderly.getBedId() != null) {
                bedMapper.update(null, new LambdaUpdateWrapper<Bed>()
                        .eq(Bed::getId, elderly.getBedId())
                        .set(Bed::getStatus, 0)); // 0-空闲
            }
            // 3. 删除老人
            this.removeById(id);
        }
    }

    @Override
    public IPage<Elderly> listElderlyWithImagingByPage(Integer pageNum, Integer pageSize){
        // 1. 处理默认值（防止前端不传参数）
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 2. 构建分页对象（Page是IPage的实现类）
        IPage<Elderly> page = new Page<>(pageNum, pageSize);

        return  elderlyMapper.listElderlyWithImagingByPage(page);
    }
}