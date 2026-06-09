package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 综合能力-专业技能项 */
@Data
@TableName("professional_skill_items")
public class ProfessionalSkillItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    /** CET4 / CET6 / COMPUTER / CERTIFICATE / ENTRANCE_EXAM */
    private String itemType;
    private String name;
    private String skillCategory;
    /** HIGH / MEDIUM / PRIMARY */
    private String skillLevel;
    /** 是否通过口语考试（CET4/CET6适用，上浮2分） */
    private Boolean oralExamPassed;
    /** 考研结果: TOOK_EXAM / PASSED_INITIAL / PASSED_REEXAM */
    private String entranceExamResult;
    private String description;
    private LocalDate occurredDate;
    private BigDecimal score;
    private String attachmentUrl;
    private String reviewStatus;
    private String reviewRemark;
    private LocalDateTime createdAt;
}
