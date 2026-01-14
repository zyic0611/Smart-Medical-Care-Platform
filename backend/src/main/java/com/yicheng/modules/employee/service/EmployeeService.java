package com.yicheng.modules.employee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.modules.employee.entity.Employee;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EmployeeService extends IService<Employee> {
    IPage<Employee> selectPage(Integer pageNum, Integer pageSize, String name);

    @Transactional(rollbackFor = Exception.class)
    void add(Employee employee);

    void update(Employee employee);

    void delete(Integer id);

    @Transactional(rollbackFor = Exception.class)
        // 这是一个写操作，必须加事务
    void deleteBatch(List<Integer> ids);

    List<Employee> selectAll();

    @Transactional(rollbackFor = Exception.class)
    void addBatch(List<Employee> list);
}
