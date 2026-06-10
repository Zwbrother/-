package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("evaluation_records")
public class EvaluationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long academicYearId;

    // ===== 基本项：品德 =====
    private BigDecimal moralAppraisalScore;   // 评议分
    private BigDecimal moralRecordScore;      // 记实分
    private BigDecimal moralTotal;            // 品德总分

    // ===== 基本项：专业素质 =====
    private BigDecimal academicWeightedAvg;   // 加权平均分

    // ===== 基本项总分 = 品德×30% + 专业×70% =====
    private BigDecimal basicTotal;
    private Integer basicRank;

    // ===== 综合能力 =====
    private Integer abilityBase;              // 能力基础分（固定75）
    private BigDecimal researchInnovation;    // 研究创新
    private BigDecimal professionalSkill;     // 专业技能
    private BigDecimal organizationWork;      // 组织工作
    private BigDecimal sportsAesthetics;      // 体育美育
    private BigDecimal laborPractice;         // 劳动教育和社会实践
    private BigDecimal abilityTotal;          // 综合能力总分
    private Integer abilityRank;

    // ===== 状态 =====
    private String status;
    private LocalDateTime submittedAt;
}
