package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("college_configs")
public class CollegeConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String collegeName;
    private String configKey;
    private String configValue;
    private String description;
}
