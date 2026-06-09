package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("graduate_exam_applications")
public class GraduateExamApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long academicYearId;
    /** DOMESTIC / OVERSEAS */
    private String examType;
    /** 是否具有复试资格 */
    private Boolean hasInterviewQualification;
    /** 是否已录取 */
    private Boolean isAdmitted;
    private String schoolName;
    private String majorName;
    /** SUBMITTED / APPROVED / REJECTED / WITHDRAWN */
    private String status;
    private String rejectReason;
    /** FIRST / SECOND */
    private String finalLevel;
    private String attachmentUrl;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long reviewerId;
}
