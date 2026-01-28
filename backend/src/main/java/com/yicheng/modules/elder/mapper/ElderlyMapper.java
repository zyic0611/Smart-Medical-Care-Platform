package com.yicheng.modules.elder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yicheng.modules.elder.pojo.entity.ElderlyEntity;
import com.yicheng.modules.elder.pojo.vo.ElderlyVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;


public interface ElderlyMapper  extends BaseMapper<ElderlyEntity> {


    /**
     * 分页查询老人信息 (包含护工、床位联查)
     * * @param page  分页参数 (必须放在第一个，MP 插件才会自动拦截并处理分页 SQL)
     * @param param 查询条件封装在 VO 或 DTO 中
     * @return 分页结果，泛型使用 ElderlyVO
     */
    IPage<ElderlyVO> selectElderlyPage(IPage<ElderlyVO> page, @Param("param")ElderlyVO param );


    IPage<ElderlyVO> listElderlyWithImagingByPage(IPage<ElderlyVO> page);

}
