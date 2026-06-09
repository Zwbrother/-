package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 品德记实项（替代原 MoralItem） */
@Data
@TableName("moral_record_items")
public class MoralRecordItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    /** VOLUNTEER / DISCIPLINE / HONOR / COLLECTIVE_HONOR */
    private String itemType;
    private String description;
    private LocalDate occurredDate;
    private BigDecimal hours;
    private BigDecimal rawValue;
    /** 荣誉等级：NATIONAL/PROVINCIAL/CITY/SCHOOL/COLLEGE */
    private String honorLevel;
    private BigDecimal score;
    private String attachmentUrl;
    /** PENDING / APPROVED / REJECTED */
    private String reviewStatus;
    private String reviewRemark;
    private LocalDateTime createdAt;
}
