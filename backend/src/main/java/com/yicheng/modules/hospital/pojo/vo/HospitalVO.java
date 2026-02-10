package com.yicheng.modules.hospital.pojo.vo;

import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import com.yicheng.modules.hospital.pojo.entity.HospitalEntity;

/**
 * (Hospital)表实体类dto
 *
 * @author zyc
 * @since 2026-02-10 14:37:49
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HospitalVO extends HospitalEntity {

	@Serial
	private static final long serialVersionUID = 1L;

    private String distance;
}

