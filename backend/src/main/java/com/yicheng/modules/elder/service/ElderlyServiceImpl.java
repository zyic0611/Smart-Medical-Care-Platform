package com.yicheng.modules.elder.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.common.RedisConstants;
import com.yicheng.modules.bed.pojo.entity.BedEntity;
import com.yicheng.modules.elder.pojo.dto.ElderlyDTO;
import com.yicheng.modules.elder.pojo.dto.ElderlyUpdateDTO;
import com.yicheng.modules.elder.pojo.entity.ElderlyEntity;
import com.yicheng.exception.CustomException;
import com.yicheng.modules.bed.mapper.BedMapper;
import com.yicheng.modules.elder.mapper.ElderlyMapper;
import com.yicheng.modules.elder.pojo.vo.ElderlyVO;
import com.yicheng.utils.CacheClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ElderlyServiceImpl extends ServiceImpl<ElderlyMapper, ElderlyEntity> implements IService<ElderlyEntity>, IElderlyService {

    private final ElderlyMapper elderlyMapper;
    private final BedMapper bedMapper;

    private final CacheClient cacheClient;


    /**
     * 1. 分页查询
     */
    @Override
    public IPage<ElderlyVO> selectElderlyPage(Integer pageNum, Integer pageSize, ElderlyVO queryParam) {
        Page<ElderlyVO> page = new Page<>(pageNum, pageSize);
        return elderlyMapper.selectElderlyPage(page, queryParam);
    }



    /**
     * 2. 新增老人 (带床位联动)
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean addElderlyWithBed(ElderlyDTO elderlyDTO) {

        // 默认入住时间为当前时间
        if (elderlyDTO.getCheckInDate() == null) elderlyDTO.setCheckInDate(LocalDate.now());

        // 默认健康
        if(elderlyDTO.getNurseId() == null) elderlyDTO.setNurseId(0);

        // 类型转换
        ElderlyEntity elderlyEntity = BeanUtil.copyProperties(elderlyDTO, ElderlyEntity.class);

        // 锁定床位
        if (elderlyEntity.getBedId() != null) updateBedStatus(elderlyEntity.getBedId(),1);

        return this.save(elderlyEntity);

    }

    /**
     * 3. 更新老人
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateElderlyWithBed(ElderlyUpdateDTO elderlyUpdateDTO) {
        // 1 查旧数据
        ElderlyEntity oldData = getById(elderlyUpdateDTO.getId());

        //2 空判断
        if (oldData == null) throw new CustomException("404","老人不存在");

        //3 床位改变
        if(!Objects.equals(oldData.getBedId(), elderlyUpdateDTO.getBedId())) {
            if(oldData.getBedId() != null) updateBedStatus(oldData.getBedId(),0);
            if(elderlyUpdateDTO.getId()!=null) updateBedStatus(elderlyUpdateDTO.getId(),1);
        }

        //4 更新

        BeanUtil.copyProperties(elderlyUpdateDTO,oldData);

        return this.updateById(oldData);
    }

    /**
     * 4. 删除老人 (退住)
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteLogic(String ids) {
        //1 转为id list
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();

        // 2. 找出这些老人占用的所有床位 ID (为了批量释放)
        // 性能优化：一次性查出所有要删除的老人，避免在循环里查库 调用mp的listByIds
        List<ElderlyEntity> elderlyList = this.listByIds(idList);

        List<Integer>bedIdsRelease=elderlyList.stream()
                .map(ElderlyEntity::getBedId)
                .filter(Objects::nonNull)//排除没有床位的老人
                .distinct()//去重
                .toList();

        //批量释放床位
        if(!bedIdsRelease.isEmpty()){
            bedMapper.update(null,new LambdaUpdateWrapper<BedEntity>()
                    .in(BedEntity::getId,bedIdsRelease)
                    .set(BedEntity::getStatus,0));
        }

        //批量删除老人
        return this.removeByIds(idList);
    }

    /*
    * 5 查看单个老人 通过redis缓存
    * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ElderlyVO detail(Long id) {
        //防止穿透的redis查询缓存
        ElderlyEntity elderlyEntity = cacheClient.queryWithPassThrough(
                RedisConstants.ELDERLY_CACHE_KEY,
                id,
                ElderlyEntity.class,
                this::getById,
                30L,
                TimeUnit.MINUTES
        );

        if(elderlyEntity == null) return null;

        return BeanUtil.copyProperties(elderlyEntity, ElderlyVO.class);
    }


    @Override
    public IPage<ElderlyVO> listElderlyWithImagingByPage(Integer pageNum, Integer pageSize){

        IPage<ElderlyVO> page = new Page<>(pageNum, pageSize);

        return  elderlyMapper.listElderlyWithImagingByPage(page);
    }


    /*
    * 辅助方法 ：根据传入的 bedId，去更新 bed 表里的 status 字段
    * */
    private void updateBedStatus(Integer bedId, Integer status) {
        bedMapper.update(null, new LambdaUpdateWrapper<BedEntity>()
                .eq(BedEntity::getId, bedId)
                .set(BedEntity::getStatus, status));
    }

}