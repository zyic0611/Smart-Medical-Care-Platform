package com.yicheng.modules.hospital.pojo.dto;

import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.math.BigDecimal;
import com.yicheng.modules.hospital.pojo.entity.HospitalEntity;

/**
 * (Hospital)表实体类dto
 *
 * @author zyc
 * @since 2026-02-10 14:37:49
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HospitalDTO extends HospitalEntity {

	@Serial
	private static final long serialVersionUID = 1L;

}

