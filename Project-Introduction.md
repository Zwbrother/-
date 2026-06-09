# 浙江工商大学本科生奖学金评选系统 — 项目介绍

> **适用对象**：浙江工商大学信息与电子工程学院  
> **政策依据**：《浙江工商大学学生素质评价办法》(2025版) 及《浙江工商大学奖学金实施办法》(2025版)  
> **系统定位**：面向本科生综合测评与奖学金评选的全流程信息化管理平台

---

## 目录

1. [项目背景与目标](#1-项目背景与目标)
2. [业务模型：综合测评体系](#2-业务模型综合测评体系)
3. [奖学金类型与评选规则](#3-奖学金类型与评选规则)
4. [系统技术架构](#4-系统技术架构)
5. [角色与权限设计](#5-角色与权限设计)
6. [核心功能模块](#6-核心功能模块)
7. [关键计算引擎](#7-关键计算引擎)
8. [数据库设计概要](#8-数据库设计概要)
9. [前端界面设计](#9-前端界面设计)
10. [技术栈全貌](#10-技术栈全貌)
11. [项目结构一览](#11-项目结构一览)
12. [部署与运行](#12-部署与运行)
13. [API 接口概览](#13-api-接口概览)
14. [项目亮点与特色](#14-项目亮点与特色)

---

## 1. 项目背景与目标

### 1.1 背景

高校学生奖学金评选是一项涉及面广、规则复杂、公平性要求高的周期性工作。传统纸质或半纸质模式存在以下痛点：

- **计算繁琐**：品德分、专业分、综合能力分等多维度加权计算，辅导员手工核分易出错
- **材料混乱**：学生提交的论文、竞赛证书、志愿服务证明等支撑材料缺乏统一归集
- **透明度不足**：评选过程不公开，容易引发学生对公平性的质疑
- **数据孤岛**：教务系统、学工系统数据不互通，需要人工搬运

本系统以 **浙江工商大学 2025 版学生素质评价办法** 和 **2025 版奖学金实施办法** 为算法蓝本，将综合测评和奖学金评选全流程数字化。

### 1.2 核心目标

| 目标 | 说明 |
|------|------|
| **自动化算分** | 根据规则引擎自动计算品德分、专业分、综合能力分，杜绝人工算分误差 |
| **一站式填报** | 学生在线完成 6 大维度评议 + 5 类能力材料的提交，辅导员在线审核 |
| **智能排名** | 系统自动按综合能力总分排序，按比例切分一/二/三等奖名额 |
| **全程可追溯** | 每步操作记录审计日志，评分结果可回溯至原始材料 |
| **公开透明** | 提供无需登录的公示页，接受全体师生监督 |

---

## 2. 业务模型：综合测评体系

系统的核心业务模型完全遵循 2025 版学生素质评价办法，构建了 **"基本项 + 综合能力"双轨评价体系**。

### 2.1 评价指标总览

```
综合测评
├── 一、基本项（达标性评价）
│   ├── 1.1 品德素质
│   │   ├── 评议成绩（70%）  — 自评5% + 学生代表60% + 辅导员35%
│   │   │   └── 6维度打分：政治素养、法治观念、心理素质、诚实守信、团队协作、社会责任
│   │   └── 记实成绩（30%）  — 基准60分 + 荣誉加分 - 处分扣分
│   │       ├── 志愿服务：按小时换算
│   │       ├── 获奖荣誉：国家级20 / 省级15 / 市级12 / 校级8
│   │       └── 违纪处分：警告-2~-4 / 严重警告-4~-6 / 记过-8~-10 / 留校察看-10
│   │
│   └── 1.2 专业素质
│       └── 加权平均分 = Σ(课程成绩 × 学分) / Σ学分
│
│   基本项总分 = 品德总分 × 30% + 专业素质 × 70%
│
└── 二、综合能力（个性发展评价）
    ├── 研究创新（30%）  — 论文/专利/竞赛/科研项目
    ├── 专业技能（25%）  — 职业技能证书/外语口试/入学考试
    ├── 组织工作（15%）  — 学生干部任职×考核等级
    ├── 体育美育（15%）  — 体育竞赛/文艺比赛获奖
    └── 劳动教育和社会实践（15%）— 社会实践/志愿服务获奖

    综合能力总分 = 75 + Σ(五项得分 × 权重)
```

### 2.2 关键评分细则

| 细则项 | 实现方式 |
|--------|----------|
| 竞赛分类系数 | A类×1.2、B类×1.0、C类×0.5 |
| 多人成果分摊 | 按排名和人数查分摊系数表（第1作者最高1.0，逐级递减） |
| 任职等级×类别 | 校级/院级/班级 × 主席/部长/干事 → 对应不同的任职分 |
| 志愿服务换算 | 按服务小时数分段线性换算 |
| 处分扣分映射 | 警告→-2、严重警告→-4、记过→-8、留校察看→-10 |

---

## 3. 奖学金类型与评选规则

### 3.1 奖学金类型

| 类型 | 等级 | 金额 | 比例 | 说明 |
|------|------|------|------|------|
| **优秀学生综合奖学金** | 一等 | 2500 元 | 3% | 德智体美劳全面优秀 |
| | 二等 | 1200 元 | 6% | |
| | 三等 | 800 元 | 12% | |
| **能力突出奖学金** | — | 800 元 | 8% | 专业学习或综合能力突出 |
| **考研奖学金** | 一等 | 600 元 | — | 升学深造 |
| | 二等 | 300 元 | — | |
| **单项奖学金** | — | — | ≥18% | 各学院自定义 |

### 3.2 综合奖学金评选流程

```
管理员创建项目 → 配置等级比例/金额
       ↓
学生填报测评 → 提交基本项 + 综合能力材料
       ↓
辅导员审核材料 → 单项通过/驳回修改
       ↓
管理员执行排名 → 系统按综合能力总分降序排序
       ↓
系统分配等级 → 按比例切分一/二/三等
       ↓
学生申报奖学金 → 查看系统推荐等级 → 确认申报
       ↓
辅导员审核申请 → 批量通过
       ↓
管理员发布公示 → 公开可查
```

### 3.3 硬性申报条件校验

系统自动校验以下条件：

- ✅ 加权平均分 ≥ 75 分
- ✅ 体育课/体质测试成绩 ≥ 80 分（免测学生除外）
- ✅ 劳动教育测评合格
- ✅ 外语成绩达标（外语平均≥75 / 一等≥80 / 高年级 CET-4 合格）
- ✅ 无未解除的处分记录
- ✅ 基本项排名 / 综合能力排名符合等级要求

---

## 4. 系统技术架构

### 4.1 架构图

```
┌─────────────────────────────────────────────────────┐
│                    前端 (React 18)                    │
│  ┌──────────┐ ┌──────────┐ ┌───────────────────┐    │
│  │ 学生端    │ │ 辅导员端  │ │ 管理员端           │    │
│  │ 测评填报  │ │ 材料审核  │ │ 项目管理/排名/公示  │    │
│  └──────────┘ └──────────┘ └───────────────────┘    │
│  ┌──────────────────────────────────────────────┐    │
│  │         公开公示页（无需登录）                 │    │
│  └──────────────────────────────────────────────┘    │
│  技术：React 18 + Vite 5 + Ant Design 5 + Zustand    │
└─────────────────────┬───────────────────────────────┘
                      │ RESTful API (JSON)
                      │ JWT Bearer Token 认证
┌─────────────────────┴───────────────────────────────┐
│                  后端 (Spring Boot 3.2)               │
│  ┌──────────┐ ┌──────────┐ ┌───────────────────┐    │
│  │ 认证层    │ │ 业务层    │ │ 计算引擎           │    │
│  │ JWT+BCrypt│ │ 测评/审核 │ │ ScoreCalcService   │    │
│  │ 角色拦截  │ │ 申报/排名 │ │ RankingService     │    │
│  └──────────┘ └──────────┘ └───────────────────┘    │
│  技术：Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 │
└─────────────────────┬───────────────────────────────┘
                      │ JDBC
┌─────────────────────┴───────────────────────────────┐
│              数据库 (H2 文件模式)                      │
│  24 张业务表，自动建表 + 演示数据注入                    │
│  文件存储：backend/data/scholarship.mv.db              │
└─────────────────────────────────────────────────────┘
```

### 4.2 架构特点

- **前后端分离**：React SPA 通过 Axios 调用 RESTful API，部署解耦
- **无状态认证**：JWT Token，服务端无需 Session 存储
- **嵌入式数据库**：H2 文件模式，零配置启动，适合校内部署
- **分层架构**：Controller → Service → Mapper，职责清晰

---

## 5. 角色与权限设计

系统设计了 **4 种角色**，每种角色拥有独立的视图和操作权限：

### 5.1 角色矩阵

| 功能模块 | 学生 | 辅导员 | 管理员 | 公众 |
|----------|:----:|:------:|:------:|:----:|
| 个人信息管理 | ✅ | ✅ | ✅ | — |
| 修改密码 | ✅ | ✅ | ✅ | — |
| 综合测评填报（品德+能力） | ✅ | — | — | — |
| 查看测评分数 | ✅ | ✅ | ✅ | — |
| 奖学金申报 | ✅ | — | — | — |
| 查看推荐等级 | ✅ | — | — | — |
| 申诉提交 | ✅ | — | — | — |
| 考研奖学金申报 | ✅ | — | — | — |
| 本院学生管理 | — | ✅ | — | — |
| 材料审核（通过/驳回） | — | ✅ | — | — |
| 申请审核/批量通过 | — | ✅ | — | — |
| 学年管理 | — | — | ✅ | — |
| 奖学金项目管理 | — | — | ✅ | — |
| 执行排名与等级分配 | — | — | ✅ | — |
| 发布公示 | — | — | ✅ | — |
| 学生代表管理 | — | — | ✅ | — |
| 数据导入（Excel/CSV） | — | — | ✅ | — |
| 查看公示名单 | — | — | — | ✅ |

### 5.2 辅导员数据隔离

辅导员仅能查看和管理 **本学院、本届** 学生的数据，系统根据辅导员的学院归属自动过滤。管理员可全局查看。

---

## 6. 核心功能模块

### 6.1 学生端

| 模块 | 页面 | 功能描述 |
|------|------|----------|
| **首页** | [Home.jsx](frontend/src/pages/student/Home.jsx) | 概览测评状态、推荐等级、待办事项 |
| **基本项测评** | [BasicEvaluation.jsx](frontend/src/pages/student/BasicEvaluation.jsx) | 品德评议（6维度×3来源打分）+ 品德记实（荣誉/志愿服务/处分记录）+ 课程成绩 |
| **综合能力** | [AbilityEvaluation.jsx](frontend/src/pages/student/AbilityEvaluation.jsx) | 5 大类能力材料填报（研究创新/专业技能/组织工作/体育美育/劳动实践） |
| **奖学金申报** | [Scholarships.jsx](frontend/src/pages/student/Scholarships.jsx) | 查看可选项目 → 自动推荐等级 → 确认申报 |
| **我的申请** | [Applications.jsx](frontend/src/pages/student/Applications.jsx) | 查看申请状态、审核进度、最终结果 |
| **考研奖学金** | [GraduateExam.jsx](frontend/src/pages/student/GraduateExam.jsx) | 考研信息填报、录取证明材料上传 |
| **申诉** | [Appeal.jsx](frontend/src/pages/student/Appeal.jsx) | 对评选结果提交申诉 |

### 6.2 辅导员端

| 模块 | 页面 | 功能描述 |
|------|------|----------|
| **学生列表** | [Students.jsx](frontend/src/pages/counselor/Students.jsx) | 查看本院本届学生及其测评状态 |
| **品德评议** | [Appraisal.jsx](frontend/src/pages/counselor/Appraisal.jsx) | 对学生进行辅导员评议打分 |
| **材料审核** | [Review.jsx](frontend/src/pages/counselor/Review.jsx) | 逐项审核学生提交的能力材料（通过/驳回+批注） |
| **申请审核** | [Applications.jsx](frontend/src/pages/counselor/Applications.jsx) | 审核奖学金申请，支持批量通过 |

### 6.3 管理员端

| 模块 | 页面 | 功能描述 |
|------|------|----------|
| **工作台** | [Dashboard.jsx](frontend/src/pages/admin/Dashboard.jsx) | 全局数据概览 |
| **学年管理** | [Years.jsx](frontend/src/pages/admin/Years.jsx) | 创建/管理学年，配置各阶段时间窗口 |
| **项目管理** | [Projects.jsx](frontend/src/pages/admin/Projects.jsx) | 创建奖学金项目、配置等级/比例/金额、发布公示 |
| **综测排名** | [Ranking.jsx](frontend/src/pages/admin/Ranking.jsx) | 查看基本项/综合能力排名，一键执行排名 |
| **学生代表** | [Representatives.jsx](frontend/src/pages/admin/Representatives.jsx) | 管理各班级学生代表（参与品德评议打分） |
| **数据导入** | [Import.jsx](frontend/src/pages/admin/Import.jsx) | CSV/Excel 导入学生数据、课程成绩 |

### 6.4 公共服务

| 功能 | 说明 |
|------|------|
| **公开公示页** | `/results` 路径，无需登录，支持按姓名/学号搜索，展示最终获奖名单 |
| **H2 控制台** | `/h2` 路径，管理员可直接查看/调试数据库 |

---

## 7. 关键计算引擎

### 7.1 ScoreCalcService — 综合测评算分引擎

位于 [ScoreCalcService.java](backend/src/main/java/com/zjsu/scholarship/service/ScoreCalcService.java)，实现了完整的评分规则：

```
输入：学生提交的评议记录 + 记实材料 + 能力材料 + 课程成绩
  │
  ├── calcMoralAppraisal(evaluationId)      → 评议成绩
  │     = 自评均分×5% + 代表评议均分×60% + 辅导员评议均分×35%
  │
  ├── calcMoralRecord(evaluationId)          → 记实成绩
  │     = 60 + 荣誉加分 - 处分扣分 + 志愿服务换算
  │
  ├── calcMoralTotal(evaluationId)           → 品德总分
  │     = 评议成绩×70% + 记实成绩×30%
  │
  ├── calcAcademicWeightedAvg(evaluationId)  → 专业素质（加权平均）
  │
  ├── calcBasicTotal(evaluationId)           → 基本项总分
  │     = 品德总分×30% + 专业素质×70%
  │
  ├── calcResearchInnovation(evaluationId)   → 研究创新得分
  ├── calcProfessionalSkill(evaluationId)    → 专业技能得分
  ├── calcOrganizationWork(evaluationId)     → 组织工作得分
  ├── calcSportsAesthetics(evaluationId)     → 体育美育得分
  ├── calcLaborPractice(evaluationId)        → 劳动实践得分
  │
  └── calcAbilityTotal(evaluationId)         → 综合能力总分
        = 75 + 研究创新×30% + 专业技能×25% + 组织工作×15%
              + 体育美育×15% + 劳动实践×15%
```

**计算触发时机**：学生每提交一条材料即自动计算单项分；辅导员审核操作后触发整体重算。

### 7.2 RankingService — 排名与等级分配引擎

位于 [RankingService.java](backend/src/main/java/com/zjsu/scholarship/service/RankingService.java)：

1. **全员重算**：遍历项目对应学年下所有已提交测评的学生，调用 ScoreCalcService 重算总分
2. **双轨排序**：分别按「基本项总分」和「综合能力总分」降序排序
3. **比例切分**：按项目配置的比例（一等 3%、二等 6%、三等 12%）切分名额
4. **条件过滤**：自动排除不满足硬性条件的学生（平均分<75、体育<80 等）
5. **等级分配**：为每位符合条件的学生分配系统推荐等级

### 7.3 CollabCoefficient — 多人成果分摊

位于 [CollabCoefficient.java](backend/src/main/java/com/zjsu/scholarship/service/CollabCoefficient.java)：

根据论文/专利/竞赛的总作者数和本人排名，查表计算分摊系数，确保多人合作成果的评分公平合理。

---

## 8. 数据库设计概要

系统共设计 **24 张表**，分为 6 大类：

| 分类 | 表名 | 说明 |
|------|------|------|
| **用户与认证** | `users` | 用户账号、角色、加密密码 |
| | `school_auth_mock` | 模拟学校统一认证初始密码 |
| **学生信息** | `students` | 学号、学院、专业、班级、CET/体育/劳动 |
| | `course_grades` | 课程成绩（课程名、学分、分数） |
| | `discipline_records` | 处分记录及解除状态 |
| **综合测评** | `evaluation_records` | 测评主表（双轨总分+排名+状态） |
| | `moral_appraisals` | 品德评议（6维度×3来源） |
| | `moral_record_items` | 品德记实（荣誉/志愿服务/处分） |
| | `research_innovation_items` | 研究创新材料 |
| | `professional_skill_items` | 专业技能材料 |
| | `organization_work_items` | 组织工作材料 |
| | `sports_aesthetics_items` | 体育美育材料 |
| | `labor_practice_items` | 劳动教育和社会实践材料 |
| **奖学金** | `scholarship_projects` | 奖学金项目定义+硬性条件 |
| | `scholarship_levels` | 奖学金等级（比例/金额/名额） |
| | `scholarship_criteria` | 奖学金评选规则 |
| | `special_scholarships` | 学院自定义单项奖学金 |
| | `applications` | 学生申报记录+评分快照 |
| | `graduate_exam_applications` | 考研奖学金申报 |
| **组织管理** | `academic_years` | 学年管理+各阶段时间窗口 |
| | `student_representatives` | 学生代表（参与评议） |
| | `college_configs` | 学院级配置 |
| **审计与申诉** | `audit_logs` | 操作审计日志 |
| | `appeal_records` | 学生申诉记录 |

数据库使用 H2 文件模式（`backend/data/scholarship.mv.db`），schema.sql 包含完整的 DDL 和增量迁移语句，data.sql 预置了演示数据。

---

## 9. 前端界面设计

### 9.1 设计规范

- **UI 框架**：Ant Design 5，企业级中后台 UI
- **布局模式**：侧边栏导航 + 顶栏面包屑
- **响应式**：适配 1920px 主流桌面分辨率
- **状态管理**：Zustand 轻量级全局状态（认证信息、角色）

### 9.2 三种角色布局

| 角色 | 布局文件 | 主导航 |
|------|----------|--------|
| 学生 | [StudentLayout.jsx](frontend/src/layouts/StudentLayout.jsx) | 首页 / 基本项测评 / 综合能力 / 奖学金申报 / 我的申请 / 考研奖学金 |
| 辅导员 | [CounselorLayout.jsx](frontend/src/layouts/CounselorLayout.jsx) | 学生管理 / 品德评议 / 材料审核 / 申请审核 |
| 管理员 | [AdminLayout.jsx](frontend/src/layouts/AdminLayout.jsx) | 工作台 / 学年管理 / 项目管理 / 综测排名 / 学生代表 / 数据导入 |

### 9.3 路由守卫

[App.jsx](frontend/src/App.jsx) 实现基于角色的路由守卫：
- 未登录用户访问任何受保护路由 → 重定向至 `/login`
- 学生访问辅导员/管理员路由 → 重定向至学生首页
- 公开路由 `/results` → 无需登录

---

## 10. 技术栈全貌

| 层次 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.2.5 |
| **语言** | Java | 17 |
| **ORM** | MyBatis-Plus | 3.5.5 |
| **数据库** | H2 (文件模式) | — |
| **认证** | JJWT + BCrypt | 0.12.5 |
| **构建工具** | Maven | — |
| **前端框架** | React | 18.3.1 |
| **构建工具** | Vite | 5.4.11 |
| **UI 组件库** | Ant Design | 5.22.5 |
| **路由** | React Router | 6.28.0 |
| **HTTP 客户端** | Axios | 1.7.9 |
| **状态管理** | Zustand | 5.0.2 |
| **日期处理** | Day.js | 1.11.13 |

---

## 11. 项目结构一览

```
zjsu-scholarship-submission/
├── backend/                                    # Spring Boot 后端
│   ├── pom.xml                                 # Maven 依赖配置
│   ├── data/scholarship.mv.db                  # H2 数据库文件（首次启动自动创建）
│   └── src/main/
│       ├── java/com/zjsu/scholarship/
│       │   ├── ScholarshipApplication.java     # Spring Boot 启动类
│       │   ├── common/                         # 统一响应 R / 全局异常处理
│       │   ├── config/                         # CORS 跨域 / Web 配置 / 密码加密
│       │   ├── security/                       # JWT 工具类 + 登录拦截器 + 权限注解
│       │   ├── entity/                         # 24 个数据实体（POJO）
│       │   ├── mapper/                         # MyBatis-Plus Mapper 接口
│       │   ├── service/                        # 核心业务逻辑
│       │   │   ├── AuthService.java            # 认证与密码管理
│       │   │   ├── ScoreCalcService.java       # ★ 综合测评算分引擎
│       │   │   ├── RankingService.java         # ★ 排名与等级分配引擎
│       │   │   ├── EvaluationService.java      # 测评记录管理
│       │   │   ├── ScholarshipService.java     # 奖学金项目管理
│       │   │   ├── DataSeedService.java        # 演示数据注入
│       │   │   ├── ImportService.java          # Excel/CSV 数据导入
│       │   │   ├── FileStorageService.java     # 附件上传存储
│       │   │   └── CollabCoefficient.java      # 多人成果分摊系数
│       │   └── controller/                     # RESTful API 控制器
│       │       ├── AuthController.java         # 登录 / 获取当前用户 / 修改密码
│       │       ├── StudentController.java      # 学生端所有接口
│       │       ├── CounselorController.java    # 辅导员端所有接口
│       │       ├── AdminController.java        # 管理员端所有接口
│       │       └── PublicController.java       # 公开公示接口
│       └── resources/
│           ├── application.yml                 # 应用配置（端口/数据库/JWT）
│           └── db/
│               ├── schema.sql                  # 24 张表 DDL + 增量迁移
│               └── data.sql                    # 演示数据（学生/课程/项目）
│
├── frontend/                                   # React 前端
│   ├── package.json                            # 依赖与脚本
│   ├── vite.config.js                          # Vite 配置（代理到 8080）
│   └── src/
│       ├── main.jsx                            # React 入口
│       ├── App.jsx                             # 路由配置 + 角色守卫
│       ├── api.js                              # Axios 实例 + JWT 拦截器
│       ├── store.js                            # Zustand 全局认证状态
│       ├── styles.css                          # 全局样式
│       ├── layouts/                            # 三种角色布局
│       │   ├── StudentLayout.jsx
│       │   ├── CounselorLayout.jsx
│       │   └── AdminLayout.jsx
│       └── pages/                              # 页面组件
│           ├── Login.jsx                       # 统一登录页
│           ├── ChangePassword.jsx              # 修改密码
│           ├── ResultsPublic.jsx               # 公开公示页
│           ├── student/                        # 学生端 7 个页面
│           ├── counselor/                      # 辅导员端 4 个页面
│           └── admin/                          # 管理员端 6 个页面
│
├── start-backend.ps1                           # 后端一键启动脚本（含 JDK/Maven 自检）
├── start-frontend.ps1                          # 前端一键启动脚本
├── student_accounts.csv                        # 学生账号导入模板
├── 2025版学生素质评选方法.md                     # 政策依据原文
├── 2025版奖学金实施方法.md                       # 政策依据原文
└── Project-Introduction.md                     # ★ 本文件
```

---

## 12. 部署与运行

### 12.1 环境要求

- **JDK 17+**（如未安装，启动脚本可自动加载 `C:\tools\jdk`）
- **Maven 3.8+**（如未安装，启动脚本可自动加载 `C:\tools\maven`）
- **Node.js 18+**（前端构建需要）

### 12.2 启动方式

**后端**（端口 9090）：
```powershell
.\start-backend.ps1
```
首次启动自动下载 Maven 依赖（约 1 分钟），看到 `Started ScholarshipApplication` 即启动成功。

**前端**（端口 5173）：
```powershell
.\start-frontend.ps1
```
浏览器访问 `http://localhost:5173/`。

### 12.3 数据管理

- 数据库文件：`backend/data/scholarship.mv.db`
- 重置数据：删除 `backend/data/` 目录后重新启动
- H2 控制台：`http://localhost:9090/h2`（JDBC URL: `jdbc:h2:file:./data/scholarship`，用户名 `sa`，密码留空）

---

## 13. API 接口概览

### 13.1 接口分类统计

| 分类 | 接口数量 | 说明 |
|------|:--------:|------|
| AuthController | 3 | 登录、获取当前用户、修改密码 |
| StudentController | ~20 | 测评填报、材料管理、奖学金申报、考研申报、申诉 |
| CounselorController | ~12 | 学生管理、评议打分、材料审核、申请审核、批量操作 |
| AdminController | ~18 | 学年管理、项目管理、排名、公示、学生代表、导入导出 |
| PublicController | 1 | 公示名单查询（无需登录） |
| **合计** | **~54** | — |

### 13.2 代表性接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `POST` | `/api/auth/login` | 公开 | 登录获取 JWT Token |
| `POST` | `/api/student/evaluation/submit` | STUDENT | 提交综合测评 |
| `POST` | `/api/student/evaluation/moral-items` | STUDENT | 添加品德记实项 |
| `POST` | `/api/student/evaluation/research-items` | STUDENT | 添加研究创新项 |
| `POST` | `/api/student/applications` | STUDENT | 提交奖学金申报 |
| `GET`  | `/api/student/scholarships/eligible` | STUDENT | 查看可申报项目 |
| `POST` | `/api/counselor/items/{kind}/{id}/review` | COUNSELOR | 审核材料 |
| `POST` | `/api/counselor/applications/batch-review` | COUNSELOR | 批量审核申请 |
| `POST` | `/api/admin/projects` | ADMIN | 创建奖学金项目 |
| `POST` | `/api/admin/projects/{id}/rank` | ADMIN | 执行排名与等级分配 |
| `POST` | `/api/admin/projects/{id}/publish` | ADMIN | 发布公示 |
| `GET`  | `/api/public/results?keyword=` | 公开 | 查询公示名单 |

---

## 14. 项目亮点与特色

### 14.1 业务层面

1. **完整落地 2025 版政策**：严格遵循学校最新版本的评价办法和奖学金实施办法，覆盖全部细则和边界条件（体育免测、处分解除、多人成果分摊、A/B/C 类竞赛系数等）。

2. **双轨制评价体系**：同时计算「基本项排名」和「综合能力排名」，支持一等/二等/三等奖学金分别设定不同的排名门槛。

3. **多类型奖学金支持**：优秀学生综合奖学金、能力突出奖学金、考研奖学金、单项奖学金，覆盖学校全部奖学金类型。

4. **全流程闭环**：从学年管理 → 测评填报 → 材料审核 → 排名分配 → 申报审核 → 结果公示，完整覆盖评选全生命周期。

### 14.2 技术层面

1. **声明式规则引擎**：评分规则通过 `ScoreCalcService` 集中管理，添加/修改评分规则只需修改计算逻辑，无需改动数据结构。

2. **快照机制**：奖学金申报时自动快照学生的基本项/综合能力总分和排名，后续修改测评不影响已提交的申报结果。

3. **灵活的时间窗口管理**：支持配置填报期、审核期、公示期的起止时间，超出窗口自动限制操作。

4. **辅导员数据隔离**：基于学院归属自动过滤数据，保证各学院辅导员间的数据安全。

5. **零配置启动**：H2 嵌入式数据库 + 自动建表 + 演示数据注入，下载即用，无需安装配置 MySQL。

### 14.3 用户体验层面

1. **自动推荐等级**：学生完成测评后，系统自动计算并推荐可申报的奖学金等级，降低认知门槛。

2. **批量操作**：辅导员支持批量审核材料、批量通过申请，大幅提升工作效率。

3. **公开透明**：公示页无需登录即可查看，支持按姓名/学号搜索，接受全体师生监督。

4. **清晰的审核流**：每条材料有独立的审核状态（待审核/已通过/已驳回+批注），学生可实时查看审核进度。

---

## 演示账号速览

| 角色 | 账号 | 密码 | 说明 |
|------|------|------|------|
| 系统管理员 | `admin` | `admin@2026` | 全局管理 |
| 辅导员 | `T2023001` | `T2023001@zjsu` | 信息学院辅导员 |
| 辅导员 | `T2023002` | `T2023002@zjsu` | 信息学院辅导员 |
| 学生 | `20231001~20231010` | `123456` | 2023 级本科生 |

> 📌 更多技术细节请参阅项目 [README.md](README.md)
