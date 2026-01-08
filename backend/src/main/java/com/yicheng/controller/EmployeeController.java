package com.yicheng.controller;



import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yicheng.common.AutoLog;
import com.yicheng.common.RequireAdmin;
import com.yicheng.common.Result;
import com.yicheng.entity.Employee;
import com.yicheng.entity.SysUser;
import com.yicheng.service.EmployeeService;
import com.yicheng.service.EmployeeServiceIml;
import com.yicheng.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/employee")
@Tag(name="护工管理模块")
public class EmployeeController {

    @Resource
    private EmployeeService employeeService;


    // 分页查询接口
    @GetMapping("/page")
    @Operation(summary = "分页查询护工")
    public Result<IPage<Employee>> selectPage(
            @RequestParam(defaultValue = "1") Integer pageNum,  // 没传就默认第1页
            @RequestParam(defaultValue = "10") Integer pageSize,// 没传就默认查10条
            @RequestParam(required = false) String name // 新增：非必填参数 便于按名字搜索
    ) {
        IPage<Employee> pageInfo = employeeService.selectPage(pageNum, pageSize, name);
        return Result.success(pageInfo);
    }

    /**
     * 查询所有接口 (给老人添加护工的下拉框用)
     */
    @GetMapping("/selectAll")
    @Operation(summary = "查询所有护工")
    public Result<List<Employee>> selectAll() {
        List<Employee> list = employeeService.selectAll();
        return Result.success(list);
    }


    //增加
    @AutoLog("新增护工")
    @RequireAdmin
    @PostMapping("add")
    @Operation(summary = "新增护工")
    public Result<?> add(@RequestBody Employee employee){

        // 1. 查一下是谁在操作
        SysUser currentUser = JwtUtils.getCurrentUser();

        // 2. 检查权限
        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("403", "无权操作，请联系管理员");
        }

        // 3. 放行
        employeeService.add(employee);
        return Result.success();
    }

    //更新
    @AutoLog("修改护工")
    @RequireAdmin
    @PutMapping("update")
    @Operation(summary = "修改护工")
    public Result<?>  update(@RequestBody Employee employee){

        // 1. 查一下是谁在操作
        SysUser currentUser = JwtUtils.getCurrentUser();

        // 2. 检查权限
        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("403", "无权操作，请联系管理员");
        }

        // 3. 放行
        employeeService.update(employee);
        return Result.success();
    }


    //删除
    @AutoLog("删除护工")
    @RequireAdmin
    @DeleteMapping("delete/{id}")
    @Operation(summary = "删除护工")
    public Result<?>  delete(@PathVariable Integer id){

        // 1. 查一下是谁在操作
        SysUser currentUser = JwtUtils.getCurrentUser();

        // 2. 检查权限
        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("403", "无权操作，请联系管理员");
        }

        // 3. 放行
        employeeService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除接口
     * @param ids 要删除的 ID 列表 (从请求体@RequestBody中获取 JSON 数组)
     */
    @AutoLog("批量删除护工")
    @RequireAdmin
    @DeleteMapping("/deleteBatch")
    @Operation(summary = "批量新增护工")
    public Result<?>  deleteBatch(@RequestBody List<Integer> ids) {

        // 1. 查一下是谁在操作
        SysUser currentUser = JwtUtils.getCurrentUser();

        // 2. 检查权限
        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("403", "无权操作，请联系管理员");
        }

        // 3. 放行


        employeeService.deleteBatch(ids);
        return Result.success();
    }


    /**
     * 导出接口
     */
    @AutoLog("导出护工信息表")
    @GetMapping("/export")
    @Operation(summary = "导出护工信息表")
    public void export(HttpServletResponse response) throws Exception {


        // 1. 从数据库查询出所有数据
        List<Employee> list = employeeService.selectAll();

        // ⬇️⬇️⬇️ 新增步骤：数据清洗 (List<Entity> -> List<Map>) ⬇️⬇️⬇️
        // 我们不直接导出 Employee 对象，而是自己组装一个 Map 列表
        // 这样想导出什么、格式怎么样，完全由我们自己控制
        List<Map<String, Object>> exportList = new ArrayList<>();

        // 定义时间格式化工具 (去掉 T，只保留年月日，或者 "yyyy-MM-dd HH:mm:ss")
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Employee employee : list) {
            // LinkedHashMap 保证列的顺序
            Map<String, Object> map = new LinkedHashMap<>();

            // 手动 put 你想导出的字段

            map.put("name", employee.getName());
            map.put("phone", employee.getPhone());
            map.put("address", employee.getAddress());
            map.put("gender", employee.getGender());
            map.put("age", employee.getAge());

            // 🚫 只要不 put "avatar"，头像就不会导出！(解决问题1)

            // 🕒 格式化时间 (解决问题2)
            if (employee.getCreateTime() != null) {
                // 把时间转成字符串，Excel 就会乖乖显示这个字符串，不会变成 xxxxx 或带 T
                map.put("createTime", formatter.format(employee.getCreateTime()));
            } else {
                map.put("createTime", "");
            }

            exportList.add(map);
        }
        // ⬆️⬆️⬆️ 清洗结束 ⬆️⬆️⬆️

        // 2. 写出到 Excel
        ExcelWriter writer = ExcelUtil.getWriter(true);

        // 3. 设置表头别名 (对应上面 map.put 的 key)

        writer.addHeaderAlias("name", "姓名");
        writer.addHeaderAlias("phone", "手机号");
        writer.addHeaderAlias("address", "地址");
        writer.addHeaderAlias("gender", "性别");
        writer.addHeaderAlias("age", "年龄");
        writer.addHeaderAlias("createTime", "入职时间");

        // ❌ 删掉头像的别名设置
        // writer.addHeaderAlias("avatar", "头像");

        writer.setOnlyAlias(true);

        // 4. 写出我们处理好的 Map 列表
        writer.write(exportList, true);

        // 自动调整列宽 (让每一列宽度刚好够用，显示更清晰)
        writer.autoSizeColumnAll();

        // 5. 设置浏览器响应 (保持不变)
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        String fileName = URLEncoder.encode("员工信息", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        ServletOutputStream out = response.getOutputStream();
        writer.flush(out, true);
        writer.close();
        out.close();
    }

    /**
     * Excel 导入接口
     * @param file 前端上传的 Excel 文件
     */

    @AutoLog("导入护工信息表")
    @RequireAdmin
    @PostMapping("/import")
    @Operation(summary = "导入护工信息表")
    public Result<?> importExcel(MultipartFile file) throws Exception {

        // 1. 查一下是谁在操作
        SysUser currentUser = JwtUtils.getCurrentUser();

        // 2. 检查权限
        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("403", "无权操作，请联系管理员");
        }

        // 3. 放行


        // 1. 获取文件输入流
        InputStream inputStream = file.getInputStream();

        // 2. 使用 Hutool 读取 Excel
        ExcelReader reader = ExcelUtil.getReader(inputStream);

        // 3. 配置表头别名 (中文表头 -> 英文属性名)
        // 必须和 Excel 里的表头文字完全一致！
//        reader.addHeaderAlias("用户名", "username");
        reader.addHeaderAlias("姓名", "name");
        reader.addHeaderAlias("手机号", "phone");
        reader.addHeaderAlias("地址", "address");
        // 性别、年龄等也可以加，看你 Excel 里有没有
        reader.addHeaderAlias("性别", "gender");
        reader.addHeaderAlias("年龄", "age");

        // 4. 读取数据，自动转换成 List<Employee> 对象
        List<Employee> list = reader.readAll(Employee.class);

        // 5. 批量插入数据库
        // 我们可以直接复用之前写的 batchDelete 的逻辑，在 Service 写一个 batchAdd
        employeeService.addBatch(list);

        return Result.success();
    }



}
