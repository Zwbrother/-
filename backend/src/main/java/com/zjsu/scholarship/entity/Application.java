package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("applications")
public class Application {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long projectId;
    private Long evaluationId;

    /** 基本项总分快照 */
    private BigDecimal snapshotBasicTotal;
    /** 基本项排名快照 */
    private Integer snapshotBasicRank;
    /** 综合能力总分快照 */
    private BigDecimal snapshotAbilityTotal;
    /** 综合能力排名快照 */
    private Integer snapshotAbilityRank;

    /** 系统推荐等级ID */
    private Long autoLevelId;
    /** 最终授予等级ID */
    private Long finalLevelId;

    /** SUBMITTED / REVIEWING / APPROVED / REJECTED / WITHDRAWN / PUBLISHED */
    private String status;
    private String rejectReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long reviewerId;
}
