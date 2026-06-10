package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("discipline_records")
public class DisciplineRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    /** WARNING(警告) / SEVERE_WARNING(严重警告) / DEMERIT(记过) / PROBATION(留校察看) / ILLEGAL(违法) */
    private String disciplineType;
    private String description;
    private LocalDate occurredDate;
    /** 是否已解除 */
    private Boolean isResolved;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
