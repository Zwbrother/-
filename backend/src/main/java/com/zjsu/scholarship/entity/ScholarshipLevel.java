package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("scholarship_levels")
public class ScholarshipLevel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String levelName;
    private Integer levelOrder;
    private BigDecimal ratio;
    private BigDecimal amount;
    private Integer quota;
    /** 该等级基本项排名限制 */
    private BigDecimal rankBasicMaxRatio;
    /** 该等级能力项排名限制 */
    private BigDecimal rankAbilityMaxRatio;
}
