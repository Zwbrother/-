package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("course_grades")
public class CourseGrade {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long academicYearId;
    private String courseName;
    private BigDecimal credit;
    private BigDecimal score;
}
