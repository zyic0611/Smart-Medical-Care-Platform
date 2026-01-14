package com.yicheng.modules.employee.service;



import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yicheng.modules.employee.entity.Employee;
import com.yicheng.exception.CustomException;
import com.yicheng.modules.employee.mapper.EmployeeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;

@Service
public class EmployeeServiceIml extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    @Resource
    private EmployeeMapper employeeMapper;



    /**
     * MyBatis-Plus 写法：分页查询
     * @param pageNum  当前第几页
     * @param pageSize 每页几条
     * @param name     查询名字 (模糊查询)
     * @return IPage (MP 自带的分页结果对象)
     */
    @Override
    public IPage<Employee> selectPage(Integer pageNum, Integer pageSize, String name) {
        // 1. 创建分页对象 (当前页, 每页大小)
        Page<Employee> page = new Page<>(pageNum, pageSize);

        // 2. 构建查询条件 (相当于 WHERE name LIKE '%xx%')
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        // 如果 name 不为空，就拼接入 SQL
        if (StringUtils.hasText(name)) {
            wrapper.like(Employee::getName, name);
        }
        // 按创建时间倒序排 (可选)
        wrapper.orderByDesc(Employee::getCreateTime);

        // 3. 调用 MP 自带的 selectPage 方法
        // 注意：这里不需要在 Mapper 里写 SQL，MP 自动生成！
        return employeeMapper.selectPage(page, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(Employee employee) {

        //1 基本参数校验
        // 防止前端传个空对象过来

        if (employee.getName() == null || "".equals(employee.getName())) {
            throw new CustomException("400", "姓名不能为空");
        }


        // 【处理入职时间】
        // 如果前端选了时间，就用前端的；如果没选(null)，就用当前时间
        if (employee.getCreateTime() == null) {
            employee.setCreateTime(LocalDate.now());
        }

       // 5. 【处理其他默认值】(可选)
//        if (employee.getAge() == null) {
//            employee.setAge(18); // 默认 18 岁
//        }

        employeeMapper.insert(employee);
    }


    @Override
    public void update(Employee employee) {
        employeeMapper.updateById(employee);
    }

    @Override
    public void delete(Integer id) {
        employeeMapper.deleteById(id);
        
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteBatch(List<Integer> ids) {
        // 参数校验
        if(ids==null||ids.isEmpty()){
            throw new CustomException("400", "请选择要删除的员工");
        }
        employeeMapper.deleteBatch(ids);
    }



    @Override
    public List<Employee> selectAll() {
        return employeeMapper.selectAll(null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addBatch(List<Employee> list) {
        //我们需要在 Service 层处理这批数据。因为是导入，可能有些必填项缺失，或者需要设置默认密码。
        for(Employee employee : list ){

            if (employee.getCreateTime() == null) {
                employee.setCreateTime(LocalDate.now()); // 默认入职时间
            }
            employeeMapper.insert(employee);
        }
    }


}
