package com.yicheng.modules.hospital.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.common.RedisConstants;
import com.yicheng.modules.hospital.pojo.entity.HospitalEntity;
import com.yicheng.modules.hospital.mapper.HospitalMapper;
import com.yicheng.modules.hospital.pojo.vo.HospitalVO;
import com.yicheng.modules.hospital.service.IHospitalService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * (Hospital)表服务实现类
 *
 * @author zyc
 * @since 2026-02-10 14:37:49
 */
@Service("hospitalService")
@RequiredArgsConstructor
public class HospitalServiceImpl extends ServiceImpl<HospitalMapper, HospitalEntity> implements IHospitalService {


    private final HospitalMapper hospitalMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public List<HospitalVO> getNearBy(Double longitude, Double latitude, Double range){

        //定义圆心和搜索半径
        Distance distance = new Distance(range, Metrics.KILOMETERS);

        //包含距离 并且由近到远
        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                .includeDistance()//算出离中心点的距离
                .sortAscending();//由近到远排序

        //redis搜索
        GeoResults<RedisGeoCommands.GeoLocation<String>>results=stringRedisTemplate.opsForGeo()
                .search(RedisConstants.HOSPITAL_GEO_KEY,
                        GeoReference.fromCoordinate(longitude,latitude)//圆心
                        ,distance,//搜索半径
                        args);//附加要求 带距离 排序
        //返回值 GeoResults 结果集合 GeoResult结果的每一行 content:point,name ;distance:
        if (results==null||results.getContent().isEmpty()){
            return Collections.emptyList();
        }

        //提取ID和距离的映射关系
        List<GeoResult<RedisCommands.GeoLocation<String>>> content = results.getContent();
        List<Long>ids = new ArrayList<>();
        Map<Long,Double> distances = new HashMap<>();

        content.forEach(r->{
            Long hospitalId=Long.valueOf(r.getContent().getName());
            ids.add(hospitalId);
            distances.put(hospitalId,r.getDistance().getValue());

        });

        //根据id批量查询医院详情
        List<HospitalEntity>hospitals=this.listByIds(ids);

        //组装VO 保持Redis返回的距离顺序
        return hospitals.stream()
                .map(
                        h->{
                            HospitalVO vo= BeanUtil.copyProperties(h,HospitalVO.class);
                            Double dis=distances.get(h.getId());
                            if(dis<1) {//保留1位小数
                                vo.setDistance((int)(dis*1000)+"m");
                            }else{
                                vo.setDistance(String.format("%.1f", dis) + "km");
                            }
                            return vo;
                        }

                ).toList();



    }


    

}

