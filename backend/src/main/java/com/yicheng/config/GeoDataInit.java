package com.yicheng.config;

import com.yicheng.common.RedisConstants;
import com.yicheng.modules.hospital.mapper.HospitalMapper;
import com.yicheng.modules.hospital.pojo.entity.HospitalEntity;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeoDataInit implements CommandLineRunner {

    @Resource
    private HospitalMapper hospitalMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public void run(String... args)  {
        List<HospitalEntity> list=hospitalMapper.selectList(null);
        //批量存入geo
        list.forEach(h->{
            stringRedisTemplate.opsForGeo().add(
                    RedisConstants.HOSPITAL_GEO_KEY,
                    new Point(h.getLongitude(),h.getLatitude()),
                    h.getId().toString()
            );
        });
    }
}
