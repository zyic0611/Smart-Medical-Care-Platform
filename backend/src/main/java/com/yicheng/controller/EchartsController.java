package com.yicheng.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yicheng.common.Result;
import com.yicheng.modules.bed.entity.Bed;
import com.yicheng.modules.elder.entity.Elderly;
import com.yicheng.modules.employee.entity.Employee;
import com.yicheng.modules.bed.mapper.BedMapper;
import com.yicheng.modules.elder.mapper.ElderlyMapper;
import com.yicheng.modules.employee.mapper.EmployeeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/echarts")
@Tag(name="图表接口")
public class EchartsController {

    private static final Logger log = LoggerFactory.getLogger(EchartsController.class);
    private static final String REDIS_KEY = "dashboard:full_data"; // 修改缓存Key

    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private ElderlyMapper elderlyMapper; // 新增
    @Resource
    private BedMapper bedMapper;         // 新增

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Operation(summary = "显示图标")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardData() {
        try {
            // 1. 查 Redis (保持你原有的优秀习惯)
            String cacheData = stringRedisTemplate.opsForValue().get(REDIS_KEY);
            if (cacheData != null && !cacheData.isEmpty()) {
                log.info("=== 驾驶舱数据走缓存 ===");
                Map<String, Object> resultMap = JSONUtil.toBean(cacheData, Map.class);
                return Result.success(resultMap);
            }

            log.info("=== 缓存未命中，开始聚合全量数据... ===");
            Map<String, Object> resultMap = new HashMap<>();

            // ================= A. 顶部卡片数据 (核心指标) =================
            // 1. 老人总数
            Long elderlyCount = elderlyMapper.selectCount(null);
            resultMap.put("elderlyCount", elderlyCount);

            // 2. 护工总数
            Long employeeCount = employeeMapper.selectCount(null);
            resultMap.put("employeeCount", employeeCount);

            // 3. 床位数据 (总数 & 已占用)
            Long totalBeds = bedMapper.selectCount(null);
            Long occupiedBeds = bedMapper.selectCount(new QueryWrapper<Bed>().eq("status", 1));
            resultMap.put("totalBeds", totalBeds);
            resultMap.put("occupiedBeds", occupiedBeds);
            // 计算入住率 (百分比)
            double occupancyRate = totalBeds == 0 ? 0 : (double) occupiedBeds / totalBeds * 100;
            resultMap.put("occupancyRate", String.format("%.1f", occupancyRate));

            // ================= B. 饼图1：老人健康评级分布 =================
            // 获取所有老人数据 (数据量大时建议用 Group By SQL，这里为了演示方便用 Java 流处理)
            List<Elderly> elderlyList = elderlyMapper.selectList(null);
            Map<String, Long> healthGroup = elderlyList.stream()
                    .filter(e -> e.getHealthStatus() != null)
                    .collect(Collectors.groupingBy(Elderly::getHealthStatus, Collectors.counting()));

            List<Map<String, Object>> healthPie = new ArrayList<>();
            healthPie.add(buildPieItem("健康 (Lv0)", healthGroup.getOrDefault("0", 0L)));
            healthPie.add(buildPieItem("一般 (Lv1)", healthGroup.getOrDefault("1", 0L)));
            healthPie.add(buildPieItem("严重 (Lv2)", healthGroup.getOrDefault("2", 0L)));
            resultMap.put("healthPie", healthPie);

            // ================= C. 饼图2：护工性别比例 (保留你原来的) =================
            List<Employee> empList = employeeMapper.selectList(null);
            long maleEmp = empList.stream().filter(e -> "男".equals(e.getGender())).count();
            long femaleEmp = empList.stream().filter(e -> "女".equals(e.getGender())).count();

            List<Map<String, Object>> empPie = new ArrayList<>();
            empPie.add(buildPieItem("男", maleEmp));
            empPie.add(buildPieItem("女", femaleEmp));
            resultMap.put("empPie", empPie);

            // ================= D. 折线图：近7天入院趋势 (模拟数据或真实查询) =================
            // 真实场景应该按 create_time group by 天，这里为了演示效果，我们生成最近7天的日期
            List<String> dates = new ArrayList<>();
            List<Integer> trends = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                String dateStr = DateUtil.format(DateUtil.offsetDay(new Date(), -i), "MM-dd");
                dates.add(dateStr);
                // 这里暂时用随机数模拟每日入院，你可以换成真实的数据库 Count
                trends.add(new Random().nextInt(10) + 1);
            }
            resultMap.put("trendDates", dates);
            resultMap.put("trendValues", trends);

            // ================= 存入 Redis (缓存 10 分钟) =================
            stringRedisTemplate.opsForValue().set(REDIS_KEY, JSONUtil.toJsonStr(resultMap), 10, TimeUnit.MINUTES);

            return Result.success(resultMap);
        } catch (Exception e) {
            log.error("驾驶舱数据获取失败", e);
            return Result.error("系统繁忙");
        }
    }

    // 辅助方法：构建饼图项
    private Map<String, Object> buildPieItem(String name, Long value) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("value", value);
        return map;
    }

    // 记得保留你原本的 refreshCache 接口，稍微改下 key 即可
    @Operation(summary = "刷新缓存")
    @GetMapping("/refreshCache")
    public Result<Void> refreshCache() {
        stringRedisTemplate.delete(REDIS_KEY);
        getDashboardData(); // 刷新
        return Result.success();
    }
}