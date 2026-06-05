package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 综合能力-研究创新项（替代原 InnovationItem） */
@Data
@TableName("research_innovation_items")
public class ResearchInnovationItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    /** COMPETITION / PAPER / PATENT / PROJECT */
    private String itemType;
    private String name;
    /** 竞赛类别 A / B / C */
    private String competitionCategory;
    /** NATIONAL / PROVINCIAL / CITY / SCHOOL / COLLEGE */
    private String levelField;
    /** FIRST / SECOND / THIRD / PARTICIPATE */
    private String awardLevel;
    /** INVENTION / UTILITY / APPEARANCE */
    private String patentType;
    /** SCHOOL_GENERAL / SCHOOL_KEY / PROVINCIAL / NATIONAL */
    private String projectLevel;
    /** APPROVED / CONCLUDED / OVERDUE */
    private String projectStatus;
    /** SCI_Q1 / SCI_Q2 / SCI_Q3 / SCI_Q4 / CSSCI / TOP / A / A_MINUS / GENERAL */
    private String journalLevel;
    private Integer totalAuthors;
    private Integer myRank;
    private Boolean hasAdvisor;
    /** 是否核心成员（核心≤20%） */
    private Boolean isCoreMember;
    private String description;
    private LocalDate occurredDate;
    private BigDecimal score;
    private String attachmentUrl;
    private String reviewStatus;
    private String reviewRemark;
    private LocalDateTime createdAt;
}
