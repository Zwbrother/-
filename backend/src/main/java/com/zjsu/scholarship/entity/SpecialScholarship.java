package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 单项奖学金定义（学院自定义） */
@Data
@TableName("special_scholarships")
public class SpecialScholarship {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long academicYearId;
    private String name;
    private String description;
    private BigDecimal amount;
    private Integer quota;
    private String status;
}
