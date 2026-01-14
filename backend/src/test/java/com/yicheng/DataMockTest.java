package com.yicheng;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yicheng.modules.bed.entity.Bed;
import com.yicheng.modules.elder.entity.Elderly;
import com.yicheng.modules.employee.entity.Employee;
import com.yicheng.modules.bed.service.BedService;
import com.yicheng.modules.elder.service.ElderlyService;
import com.yicheng.modules.employee.service.EmployeeService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class DataMockTest {

    @Resource
    private ElderlyService elderlyService;

    @Resource
    private BedService bedService;

    @Resource
    private EmployeeService employeeService;

    // === 1. 公共工具库 (姓名、地址等) ===
    private final Random random = new Random();

    private final String[] surnames = {
            "赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈", "褚", "卫", "蒋", "沈", "韩", "杨",
            "朱", "秦", "尤", "许", "何", "吕", "施", "张", "孔", "曹", "严", "华", "金", "魏", "陶", "姜",
            "戚", "谢", "邹", "喻", "柏", "水", "窦", "章", "云", "苏", "潘", "葛", "奚", "范", "彭", "郎",
            "鲁", "韦", "昌", "马", "苗", "凤", "花", "方", "俞", "任", "袁", "柳", "邓", "鲍", "史", "唐",
            "费", "廉", "岑", "薛", "雷", "贺", "倪", "汤", "滕", "殷", "罗", "毕", "郝", "邬", "安", "常"
    };

    private final String[] words = {
            "建", "国", "爱", "民", "庆", "秀", "英", "桂", "兰", "强", "军", "平", "伟", "芳", "娜",
            "敏", "静", "淑", "芬", "铁", "柱", "刚", "勇", "毅", "峰", "磊", "洋", "艳", "丽", "梅",
            "杰", "辉", "春", "夏", "秋", "冬", "雪", "海", "江", "成", "龙", "虎", "波", "宁", "宏",
            "宇", "超", "明", "霞", "云", "莲", "真", "环", "雪", "荣", "爱", "妹", "霞", "香", "月",
            "莺", "媛", "艳", "瑞", "凡", "佳", "涛", "昌", "进", "林", "有", "坚", "和", "彪", "博"
    };

    private final String[] addresses = {
            "幸福小区", "阳光花园", "金桥国际", "龙湖天街", "万达广场", "绿地世纪城", "滨江一号", "锦绣中华",
            "蓝天公寓", "碧水湾", "东方曼哈顿", "汤臣一品", "紫金山庄"
    };

    /**
     * 工具方法：生成随机姓名
     */
    private String generateName() {
        String surname = surnames[random.nextInt(surnames.length)];
        if (random.nextBoolean()) {
            return surname + words[random.nextInt(words.length)] + words[random.nextInt(words.length)];
        } else {
            return surname + words[random.nextInt(words.length)];
        }
    }

    /**
     * 工具方法：生成随机手机号
     */
    private String generatePhone() {
        String[] prefix = {"135", "136", "137", "138", "139", "150", "151", "152", "157", "158", "159", "182", "183", "187", "188"};
        String header = prefix[random.nextInt(prefix.length)];
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            body.append(random.nextInt(10));
        }
        return header + body.toString();
    }

    // === 2. 核心测试方法 ===

    /**
     * 功能一：生成随机老人数据
     * 生成数量：200 位
     */
    @Test
    public void mockElderlyData() {
        System.out.println("👴 正在清空旧老人数据...");
        elderlyService.remove(new QueryWrapper<>());

        System.out.println("🚀 开始生成 200 位老人数据...");
        long start = System.currentTimeMillis();
        List<Elderly> batchList = new ArrayList<>();

        for (int i = 0; i < 200; i++) {
            Elderly elderly = new Elderly();

            // 1. 基础信息
            elderly.setName(generateName());
            elderly.setGender(random.nextBoolean() ? "男" : "女");
            elderly.setAge(60 + random.nextInt(41)); // 60-100岁

            // 2. 健康状态 (概率控制：70%健康，20%一般，10%严重)
            int chance = random.nextInt(100);
            if (chance < 70) {
                elderly.setHealthStatus("0");
            } else if (chance < 90) {
                elderly.setHealthStatus("1");
            } else {
                elderly.setHealthStatus("2");
            }

            // 3. 手机号 & 地址 (如果你的实体类有这些字段的话)
            // elderly.setPhone(generatePhone());
            // elderly.setAddress(addresses[random.nextInt(addresses.length)] + random.nextInt(100) + "号");

            // 4. 入住时间 (最近10年)
            elderly.setCreateTime(LocalDate.now().minusDays(random.nextInt(365 * 10)));

            // 5. 关联字段 (默认为空，等待分配)
            // elderly.setBedId(null);
            // elderly.setNurseId(null);

            batchList.add(elderly);
        }

        elderlyService.saveBatch(batchList);
        long end = System.currentTimeMillis();
        System.out.println("✅ 老人数据生成完成！耗时：" + (end - start) + "ms");
    }

    /**
     * 功能二：生成床位数据
     * 生成规则：5栋楼(A-E) * 5层 * 20间 = 500张床位
     */
    /**
     * 功能二：生成标准化床位数据
     * 规则：5栋楼(A-E) * 5层 * 10间房/层 * 2张床/房 = 500张床位
     * 格式：楼栋-楼层-房间号-床号 (例如: A-1-101-1)
     */
    @Test
    public void mockBedData() {
        System.out.println("🛏️ 正在重置并清空床位数据...");
        // 建议：如果你已经按照我之前的建议修改了实体类，这里会自动处理自增 ID
        bedService.remove(new QueryWrapper<>());

        System.out.println("🚀 开始生成 500 张标准化床位...");
        long start = System.currentTimeMillis();
        List<Bed> batchList = new ArrayList<>();

        String[] buildings = {"A", "B", "C", "D", "E"};

        for (String building : buildings) {
            for (int floor = 1; floor <= 5; floor++) {
                // 每层楼设为 10 个房间，这样每层 20 张床，5 层正好 100 张，5 栋楼共 500 张
                for (int roomNum = 1; roomNum <= 10; roomNum++) {
                    // 生成房间号：101, 102... 510
                    String roomId = String.format("%d%02d", floor, roomNum);

                    // 每个房间放 2 张床
                    for (int bedIndex = 1; bedIndex <= 2; bedIndex++) {
                        Bed bed = new Bed();

                        // 核心修改：组合成标准化格式 A-1-101-1
                        String finalBedNumber = building + "-" + floor + "-" + roomId + "-" + bedIndex;

                        bed.setBedNumber(finalBedNumber);
                        bed.setStatus(0); // 初始全部为空闲

                        batchList.add(bed);
                    }
                }
            }
        }

        // 批量插入数据库
        bedService.saveBatch(batchList);

        long end = System.currentTimeMillis();
        System.out.println("✅ 标准化床位数据生成完成！");
        System.out.println("📊 生成总数：" + batchList.size());
        System.out.println("⏱️ 耗时：" + (end - start) + "ms");
    }

    /**
     * 功能三：生成护工数据
     * 生成数量：50 位
     */
    @Test
    public void mockEmployeeData() {
        System.out.println("👨‍⚕️ 正在清空旧护工数据...");
        employeeService.remove(new QueryWrapper<>());

        System.out.println("🚀 开始生成 50 名护工...");
        long start = System.currentTimeMillis();
        List<Employee> batchList = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            Employee emp = new Employee();

            emp.setName(generateName());
            // 护工多为女性 (70%概率)
            emp.setGender(random.nextInt(10) < 7 ? "女" : "男");
            emp.setAge(25 + random.nextInt(31)); // 25-55岁
            emp.setPhone(generatePhone());
            emp.setAddress(addresses[random.nextInt(addresses.length)] + (random.nextInt(20) + 1) + "栋");
            emp.setCreateTime(LocalDate.now().minusDays(random.nextInt(365 * 3)));

            // 头像暂空，或者设置一个默认的MinIO地址
            emp.setAvatar(null);

            batchList.add(emp);
        }

        employeeService.saveBatch(batchList);
        long end = System.currentTimeMillis();
        System.out.println("✅ 护工数据生成完成！耗时：" + (end - start) + "ms");
    }
}