package com.yicheng.modules.bed.service;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.common.RedisConstants;
import com.yicheng.modules.bed.pojo.dto.BedAddDTO;
import com.yicheng.modules.bed.pojo.dto.BedUpdateDTO;
import com.yicheng.modules.bed.pojo.entity.BedEntity;
import com.yicheng.exception.CustomException;
import com.yicheng.modules.bed.mapper.BedMapper;
import com.yicheng.modules.bed.pojo.vo.BedVO;
import com.yicheng.modules.sysdict.service.ISysDictService;
import com.yicheng.utils.CacheClient;
import com.yicheng.utils.Func;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BedServiceImpl extends ServiceImpl<BedMapper, BedEntity> implements IBedService {


    private final CacheClient cacheClient;

    private final ISysDictService sysDictService;

    //分页查询
    @Override
    public IPage<BedVO> selectBedPage(IPage<BedVO>page, BedVO bedVO){
        IPage<BedVO> result=page.setRecords(baseMapper.selectBedPage(page,bedVO));
        this.setParaStr(result.getRecords());
        return  result;
    }


    @Override
    public List<BedVO> selectFreeBeds(BedVO bedVO)
    {
        return baseMapper.selectFreeBeds(bedVO);
    }


    /**
     * 新增床位
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(BedAddDTO bedAddDTO) {
        //1. 业务校验：查重：利用 MyBatis-Plus 的 lambdaQuery 快速查询
        Long count=lambdaQuery().eq(BedEntity::getBedNumber,bedAddDTO.getBedNumber()).count();
        if(count>0){
            throw new CustomException("400","床位号"+bedAddDTO.getBedNumber()+"已占用");
        }

        //2 .实体转换 DTO -> Entity
        BedEntity bedEntity = new BedEntity();
        // 使用 Spring 提供的工具类进行拷贝 (把 DTO 的属性存进 Entity)
        BeanUtil.copyProperties(bedAddDTO, bedEntity);


        if(bedEntity.getStatus()==null){
            bedEntity.setStatus(0);//默认设置为空闲床位;
        }
        return super.save(bedEntity);
    }

    /*
    * 修改床位
    * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateByDTO(BedUpdateDTO bedUpdateDTO){
        //1, 安全检查 检查ID是否存在
        BedEntity dbBedEntity =baseMapper.selectById(bedUpdateDTO.getId());
        if (dbBedEntity == null) {
            throw new CustomException("404", "要修改的床位不存在");
        }

        //2 业务检查 如果修改了床位号 需要重新查重
        //如果改的床位号非空 并且床位号确实修改了
        if(bedUpdateDTO.getBedNumber()!=null&&!bedUpdateDTO.getBedNumber().equals(dbBedEntity.getBedNumber())){
            Long count=lambdaQuery().eq(BedEntity::getBedNumber,bedUpdateDTO.getBedNumber()).count();
            if(count>0){
                throw new CustomException("400", "新的床位号 " + bedUpdateDTO.getBedNumber() + " 已被占用");
            }
        }

        //3 转换并更新
        BedEntity bedEntity = new BedEntity();
        BeanUtil.copyProperties(bedUpdateDTO, bedEntity);

        //4 先更新数据库 根据ID进行更新非NULL的字段
        boolean isSuccess=this.updateById(bedEntity);

        //5 如果更新成功 删除缓存
        String key= RedisConstants.BED_CACHE_KEY+ bedEntity.getId();
        if(isSuccess){
            cacheClient.delete(key);
        }

        return isSuccess;

    }

    /*
    * 批量逻辑删除
    * */

    @Override
    public boolean deleteLogic(String ids){

        List<Long> idList = Func.toLongList(ids);

        if(Func.isEmpty(idList)){
            return false;
        }

        //一次查出所有的床位 避免N+1
        List<BedEntity> bedToCheck = this.listByIds(idList);

        for(BedEntity bedEntity : bedToCheck){
            if(bedEntity.getStatus()==1){
                throw new CustomException("400", "床位 " + bedEntity.getBedNumber() + " 正在使用，无法删除");
            }
        }

        // 4. 执行批量删除
        boolean isSuccess = this.removeByIds(idList);

        // 5. 批量删除缓存 (循环删 Redis 问题不大，因为 Redis 极快
        if(isSuccess){
            idList.forEach(id -> cacheClient.delete(RedisConstants.BED_CACHE_KEY + id));
        }

        return isSuccess;
    }

    @Override
    public BedVO detail(Long id) {

        //使用防止穿透的缓存查询
        BedEntity bedEntity =cacheClient.queryWithPassThrough(
                RedisConstants.BED_CACHE_KEY,
                id,
                BedEntity.class,
                this::getById,
                30L,
                TimeUnit.MINUTES
        );

        if(bedEntity ==null){
            return null;//如果查到空对象 直接返回
        }

        BedVO bedVO = BeanUtil.copyProperties(bedEntity,BedVO.class);
        this.setParaStr(Collections.singletonList(bedVO));
        return bedVO;


    }


    private void setParaStr(List<BedVO> list){

        if(Func.isEmpty(list)){
            return;
        }

        //内存组装
        list.forEach(vo -> {
            //填充字典
            vo.setStatusStr(sysDictService.getValue("bed_status",Func.toStr(vo.getStatus())));
        });
    }



}
