package com.yicheng.modules.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yicheng.modules.employee.entity.Employee;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EmployeeMapper extends BaseMapper<Employee> {

    //加上 @Param("name")，明确告诉 XML：这个变量叫 "name" ！！！！很重要
    List<Employee> selectAll(@Param("name")String name);




    //根据Id更新数据
    int updateById(Employee employee);

    //根据Id删除
    int deleteById(Integer id);

    // 用来查重
    Employee selectByUsername(String name);

    /**
     * 批量删除
     * @param ids 要删除的 ID 列表
     * @return 影响的行数
     */
    int deleteBatch(@Param("list") List<Integer> ids);

    Employee selectById(Integer integer);


}
