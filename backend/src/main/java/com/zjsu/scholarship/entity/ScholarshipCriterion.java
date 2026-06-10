package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("scholarship_criteria")
public class ScholarshipCriterion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    /** TOTAL_RANK_TOP_RATIO / ACADEMIC_MIN / NO_FAIL / NO_DISCIPLINE / DEGREE_TYPE / GRADE / INNOVATION_MIN */
    private String ruleType;
    private String ruleValue;
}
