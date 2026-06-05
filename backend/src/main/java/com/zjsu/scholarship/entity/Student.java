package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("students")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String studentNo;
    private String name;
    private String gender;
    private String college;
    private String major;
    private String grade;
    private String className;
    private String dormNo;
    /** CET-4 成绩 */
    private Integer cet4Score;
    /** CET-6 成绩（0 表示未报考） */
    private Integer cet6Score;
    /** 体育课/体测成绩 */
    private java.math.BigDecimal peScore;
    /** 劳动教育测评结果 PASS / FAIL / PENDING */
    private String laborEvaluation;
}
