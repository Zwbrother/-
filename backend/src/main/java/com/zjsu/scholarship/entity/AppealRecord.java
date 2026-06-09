package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appeal_records")
public class AppealRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private Long studentId;
    private Long projectId;
    /** COLLEGE / UNIVERSITY */
    private String appealLevel;
    private String reason;
    /** PENDING / PROCESSING / RESOLVED / REJECTED */
    private String status;
    private String response;
    private LocalDateTime submittedAt;
    private LocalDateTime respondedAt;
}
