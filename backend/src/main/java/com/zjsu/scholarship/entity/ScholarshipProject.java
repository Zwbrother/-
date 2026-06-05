package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("scholarship_projects")
public class ScholarshipProject {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long academicYearId;
    /**
     * COMPREHENSIVE（优秀学生综合奖学金）/ ABILITY（能力突出奖学金）/
     * GRADUATE_EXAM（考研奖学金）/ SPECIAL（单项奖学金）/ NATIONAL（国家奖学金）/
     * PROVINCIAL（省政府奖学金）/ NAMED（专项奖学金）
     */
    private String typeCode;
    private String projectName;
    private String description;
    private LocalDateTime applyStartAt;
    private LocalDateTime applyEndAt;
    /** DRAFT / OPEN / REVIEWING / PUBLISHED / CLOSED */
    private String status;
    private Boolean ranked;
    /** 申报硬性条件 */
    private BigDecimal minWeightedAvg;
    private BigDecimal minPeScore;
    private Boolean needLaborPass;
    private String foreignLangRequirement;
    private Boolean noDiscipline;
    private String remark;
}
