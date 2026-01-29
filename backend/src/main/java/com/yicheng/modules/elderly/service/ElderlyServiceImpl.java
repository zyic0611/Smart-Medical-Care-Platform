package com.yicheng.modules.elderly.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.common.RedisConstants;
import com.yicheng.modules.bed.pojo.entity.BedEntity;
import com.yicheng.modules.bed.service.IBedService;
import com.yicheng.modules.elderly.pojo.dto.ElderlyDTO;
import com.yicheng.modules.elderly.pojo.dto.ElderlyUpdateDTO;
import com.yicheng.modules.elderly.pojo.entity.ElderlyEntity;
import com.yicheng.exception.CustomException;
import com.yicheng.modules.bed.mapper.BedMapper;
import com.yicheng.modules.elderly.mapper.ElderlyMapper;
import com.yicheng.modules.elderly.pojo.vo.ElderlyVO;
import com.yicheng.modules.employee.entity.Employee;
import com.yicheng.modules.employee.service.EmployeeService;
import com.yicheng.modules.sysdict.mapper.SysDictMapper;
import com.yicheng.modules.sysdict.pojo.entity.SysDict;
import com.yicheng.modules.sysdict.service.ISysDictService;
import com.yicheng.utils.CacheClient;
import com.yicheng.utils.Func;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElderlyServiceImpl extends ServiceImpl<ElderlyMapper, ElderlyEntity> implements IService<ElderlyEntity>, IElderlyService {

    //操作表数据
    private final ElderlyMapper elderlyMapper;
    private final BedMapper bedMapper;

    //字典
    private final ISysDictService sysDictService;

    //外键
    private final IBedService bedService;
    private final EmployeeService employeeService;

    private final CacheClient cacheClient;


    /**
     * 1. 分页查询
     */
    @Override
    public IPage<ElderlyVO> selectElderlyPage(Integer pageNum, Integer pageSize, ElderlyVO queryParam) {
        Page<ElderlyVO> page = new Page<>(pageNum, pageSize);
        IPage<ElderlyVO> result = elderlyMapper.selectElderlyPage(page, queryParam);
        //包装
        this.setParaStr(result.getRecords());
        return result;
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
            if(elderlyUpdateDTO.getId()!=null) updateBedStatus(elderlyUpdateDTO.getBedId(),1);
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
        List<Long> idList = Func.toLongList(ids);

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

        ElderlyVO elderlyVO = BeanUtil.copyProperties(elderlyEntity, ElderlyVO.class);

        //包装
        this.setParaStr(Collections.singletonList(elderlyVO));

        return elderlyVO;

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


    private void setParaStr(List<ElderlyVO> list){

        if(Func.isEmpty(list)){
            return;
        }

        //1查询出床位
        //1 批量查询所有的床位
        List<Integer>bedIds=list.stream()
                .map(ElderlyVO::getBedId)
                .filter(Func::isNotEmpty)
                .distinct()
                .toList();

        Map<Integer,String> bedMap;
        if(Func.isNotEmpty(bedIds)){
            //转换出Map<bedId,bedNumber>
            bedMap=bedService.listByIds(bedIds).stream()
                    .collect(Collectors.toMap(BedEntity::getId,BedEntity::getBedNumber));
        }else{
            bedMap = new HashMap<>(); // 防止空指针
        }

        //2 批量查出所有的护工
        List<Integer>employeeIds=list.stream()
                .map(ElderlyVO::getNurseId)
                .filter(Func::isNotEmpty)
                .distinct()
                .toList();

        Map<Integer,String> employeeNameMap;
        Map<Integer,String> employeePhoneMap;
        if(Func.isNotEmpty(employeeIds)){
            employeeNameMap=employeeService.listByIds(employeeIds).stream()
                    .collect(Collectors.toMap(Employee::getId,Employee::getName));

            employeePhoneMap=employeeService.listByIds(employeeIds).stream()
                    .collect(Collectors.toMap(Employee::getId,Employee::getPhone));
        }else{
            employeeNameMap=new HashMap<>();
            employeePhoneMap=new HashMap<>();
        }

        //3 内存组装
        list.forEach(vo -> {

            //填充字典
            vo.setGenderStr(sysDictService.getValue("gender",Func.toStr(vo.getGender())));
            vo.setHealthStatusStr(sysDictService.getValue("health_status",Func.toStr(vo.getHealthStatus())));

            //填充外键
            vo.setBedNumber(bedMap.getOrDefault(vo.getBedId(),"未分配"));
            vo.setNurseName(employeeNameMap.getOrDefault(vo.getNurseId(),"暂无护工"));
            vo.setNursePhone(employeePhoneMap.getOrDefault(vo.getNurseId(),"暂无电话"));
        });
    }

}