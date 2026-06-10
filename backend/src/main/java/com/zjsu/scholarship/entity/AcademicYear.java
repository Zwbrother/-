package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("academic_years")
public class AcademicYear {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String yearName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime fillStartAt;
    private LocalDateTime fillEndAt;
    private LocalDateTime reviewStartAt;
    private LocalDateTime reviewEndAt;
    private LocalDateTime publicStartAt;
    private LocalDateTime publicEndAt;
}
