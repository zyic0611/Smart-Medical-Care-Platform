package com.yicheng.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.yicheng.common.Result;
import com.yicheng.entity.Employee;
import com.yicheng.mapper.EmployeeMapper; // 偷懒直接用 Mapper，标准应该用 Service
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/echarts")
public class EchartsController {

    @Resource
    private EmployeeMapper employeeMapper;


    @Resource
    private StringRedisTemplate stringRedisTemplate; // ⬇️ 注入 Redis 工具


    @GetMapping("/data")
    public Result<Map<String, Object>> getEchartsData() {

        // ================= 1. 先查 Redis 缓存 =================
        String redisKey = "dashboard:sex:rate"; // 定义一个唯一的 Key
        String cacheData = stringRedisTemplate.opsForValue().get(redisKey);

        if (cacheData != null) {
            // 如果缓存里有，直接把字符串转回 Map 返回
            System.out.println("=== 走了 Redis 缓存，没查数据库 ===");
            Map<String, Object> resultMap = JSONUtil.toBean(cacheData, Map.class);
            return Result.success(resultMap);
        }


        // ================= 2. 缓存没有，去查数据库 =================
        System.out.println("=== 没缓存，正在查询数据库... ===");



        List<Employee> list = employeeMapper.selectAll(null); // 查所有

        // 1. 统计男女各多少人
        int maleCount = 0;
        int femaleCount = 0;

        for (Employee employee : list) {
            String gender = employee.getGender();
            if ("男".equals(gender)) {
                maleCount++;
            } else if ("女".equals(gender)) {
                femaleCount++;
            }
        }

        // 2. 组装成 ECharts 饼图需要的数据格式
        // 格式：[ { value: 10, name: '男' }, { value: 20, name: '女' } ]
        List<Map<String, Object>> pieList = new ArrayList<>();

        Map<String, Object> maleMap = new HashMap<>();
        maleMap.put("name", "男");
        maleMap.put("value", maleCount);
        pieList.add(maleMap);

        Map<String, Object> femaleMap = new HashMap<>();
        femaleMap.put("name", "女");
        femaleMap.put("value", femaleCount);
        pieList.add(femaleMap);

        // 3. 返回给前端
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("pie", pieList); // 饼图数据


        // ================= 3. 查完存入 Redis (关键！) =================
        // 把结果对象转成 JSON 字符串
        String jsonStr = JSONUtil.toJsonStr(resultMap);

        // 存入 Redis，并设置 30 分钟过期 (防止数据一直旧)
        // set(Key, Value, Time, Unit)
        stringRedisTemplate.opsForValue().set(redisKey, jsonStr, 30, TimeUnit.MINUTES);


        return Result.success(resultMap);
    }
}