package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 综合能力-体育美育项 */
@Data
@TableName("sports_aesthetics_items")
public class SportsAestheticsItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    /** SPORTS / AESTHETICS */
    private String itemType;
    private String name;
    /** NATIONAL / PROVINCIAL / CITY / SCHOOL / COLLEGE */
    private String levelField;
    /** FIRST / SECOND / THIRD */
    private String awardLevel;
    private Boolean isTeam;
    private Boolean isCoreMember;
    private Integer teamSize;
    private String description;
    private LocalDate occurredDate;
    private BigDecimal score;
    private String attachmentUrl;
    private String reviewStatus;
    private String reviewRemark;
    private LocalDateTime createdAt;
}
