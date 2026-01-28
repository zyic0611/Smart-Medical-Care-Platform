package com.yicheng.modules.elder.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yicheng.modules.bed.pojo.vo.BedVO;
import com.yicheng.modules.elder.pojo.dto.ElderlyDTO;
import com.yicheng.modules.elder.pojo.dto.ElderlyUpdateDTO;
import com.yicheng.modules.elder.pojo.entity.ElderlyEntity;
import com.yicheng.modules.elder.pojo.vo.ElderlyVO;
import org.springframework.transaction.annotation.Transactional;

public interface IElderlyService extends IService<ElderlyEntity> {

    IPage<ElderlyVO> selectElderlyPage(Integer pageNum, Integer pageSize, ElderlyVO queryParam);


    boolean addElderlyWithBed(ElderlyDTO  elderlyDTO);


    boolean updateElderlyWithBed(ElderlyUpdateDTO elderlyUpdateDTO);


    boolean deleteLogic(String ids);

    ElderlyVO detail(Long id);



    IPage<ElderlyVO> listElderlyWithImagingByPage(Integer pageNum, Integer pageSize);
}
