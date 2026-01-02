package com.yicheng.mapper;

import com.yicheng.entity.Employee;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface EmployeeMapper {

    //加上 @Param("name")，明确告诉 XML：这个变量叫 "name" ！！！！很重要
    List<Employee> selectAll(@Param("name")String name);


    void insert(Employee employee);

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
