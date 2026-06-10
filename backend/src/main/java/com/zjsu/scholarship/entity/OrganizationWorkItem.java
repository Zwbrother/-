package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 综合能力-组织工作项 */
@Data
@TableName("organization_work_items")
public class OrganizationWorkItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    private String name;
    /** SCHOOL / COLLEGE / CLASS / CLUB */
    private String orgLevel;
    private String positionName;
    /** 任职岗位分 9~18 */
    private Integer positionScore;
    /** EXCELLENT / COMPETENT / INCOMPETENT */
    private String performanceGrade;
    /** 绩效分 */
    private Integer performanceScore;
    /** 任职月数 */
    private Integer durationMonths;
    private String description;
    private LocalDate occurredDate;
    private BigDecimal score;
    private String attachmentUrl;
    private String reviewStatus;
    private String reviewRemark;
    private LocalDateTime createdAt;
}
