package com.zjsu.scholarship.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("school_auth_mock")
public class SchoolAuthMock {
    @TableId
    private String account;
    private String initialPassword;
}
