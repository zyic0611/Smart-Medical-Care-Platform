package com.yicheng.service;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yicheng.entity.Employee;
import com.yicheng.exception.CustomException;
import com.yicheng.mapper.EmployeeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmployeeService {

    @Resource
    private EmployeeMapper employeeMapper;



    /**
     * 分页查询
     * @param pageNum  当前第几页
     * @param pageSize 每页几条
     * @return PageInfo (包含：总条数 total, 当前页数据 list, 总页数 pages ...)
     */
    public PageInfo<Employee> selectPage(Integer pageNum, Integer pageSize,String name) {
        // 1. 开启分页 (这句代码必须紧贴在查询语句之前！)
        // 它会告诉 MyBatis：“下一条 SQL 给我加上 LIMIT”
        PageHelper.startPage(pageNum, pageSize);

        // 2. 正常调用查询 (不需要改 SQL，PageHelper 会自动拦截)
        List<Employee> list = employeeMapper.selectAll(name);

        // 3. 用 PageInfo 包装结果
        // PageInfo 会自动计算 total (总条数), pages (总页数) 等信息
        return new PageInfo<>(list);
    }

    @Transactional(rollbackFor = Exception.class)
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


    public void update(Employee employee) {
        employeeMapper.updateById(employee);
    }

    public void delete(Integer id) {
        employeeMapper.deleteById(id);
        
    }

    @Transactional(rollbackFor = Exception.class) // 这是一个写操作，必须加事务
    public void deleteBatch(List<Integer> ids) {
        // 参数校验
        if(ids==null||ids.isEmpty()){
            throw new CustomException("400", "请选择要删除的员工");
        }
        employeeMapper.deleteBatch(ids);
    }



    public List<Employee> selectAll() {
        return employeeMapper.selectAll(null);
    }

    @Transactional(rollbackFor = Exception.class)
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
