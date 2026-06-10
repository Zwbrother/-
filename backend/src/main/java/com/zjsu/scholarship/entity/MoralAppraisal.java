package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 品德评议（6维度 × 3来源 = 自评/学生代表/辅导员） */
@Data
@TableName("moral_appraisals")
public class MoralAppraisal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    /** SELF / STUDENT_REP / COUNSELOR */
    private String appraiserType;
    /** 政治素养 0-20 */
    private BigDecimal politicalLiteracy;
    /** 法治观念 0-20 */
    private BigDecimal legalAwareness;
    /** 心理素质 0-20 */
    private BigDecimal mentalQuality;
    /** 诚实守信 0-20 */
    private BigDecimal integrityScore;
    /** 团队协作 0-20 */
    private BigDecimal teamwork;
    /** 社会责任 0-20 */
    private BigDecimal socialResponsibility;
    /** 六维度合计 */
    private BigDecimal total;
    private LocalDateTime createdAt;
}
