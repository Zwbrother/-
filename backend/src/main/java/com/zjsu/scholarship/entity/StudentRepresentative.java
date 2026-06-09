package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_representatives")
public class StudentRepresentative {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long academicYearId;
    private Long studentId;
    private String className;
    private LocalDateTime electedAt;
}
