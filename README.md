# 浙江工商大学本科生奖学金评选系统

> 信息与电子工程学院 · 本科生综合测评 + 奖学金评选 · 全栈实现
> 依据《浙江工商大学学生素质评价办法》(2025版) 及《浙江工商大学奖学金实施办法》(2025版) 开发

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 |
| 数据库 | H2（文件模式，零配置，自动建表+灌入演示数据） |
| 认证 | JWT + BCrypt（密码加密） |
| 前端 | React 18 + Vite 5 + Ant Design 5 + Zustand |
| 接口 | RESTful，前后端分离 |

---

## 一键运行

> 本仓库已自带 JDK / Maven 的下载配置脚本。若机器上尚未安装 JDK 17 + Maven，可先执行 `start-backend.ps1`，脚本会自动加载 `C:\tools\jdk` 与 `C:\tools\maven`。

### 启动后端（端口 8080）

```powershell
.\start-backend.ps1
```

首次启动会下载 Spring Boot 依赖（约 1 分钟）。看到 `Started ScholarshipApplication` 即启动成功。

数据库文件存于 `backend/data/scholarship.mv.db`，演示数据自动 MERGE。

### 启动前端（端口 5173）

新开一个 PowerShell 窗口：

```powershell
.\start-frontend.ps1
```

浏览器访问 **http://localhost:5173/**

---

## 演示账号

| 角色 | 账号 | 初始密码 | 说明 |
|------|------|---------|------|
| 系统管理员 | `admin` | `admin@2026` | 学年/项目管理 |
| 辅导员 | `T2023001` | `T2023001@zjsu` | 材料审核/申请审核 |
| 辅导员 | `T2023002` | `T2023002@zjsu` | 材料审核/申请审核 |
| 学生 张明 | `20231001` | `123456` | 大二 人工智能 |
| 学生 吴磊 | `20231008` | `123456` | 大二 电子信息工程 |
| 其他学生 | `20231002 ~ 20231010` | `123456` | — |

> 登录账号即学校统一身份认证账号。学生用学号，辅导员用工号，初始密码与学校其他系统一致；登录后可在「修改密码」中改为自定义密码，**不强制**修改。

公开公示页地址（无需登录）：**http://localhost:5173/results**

---

## 系统设计要点

### 1. 综合测评模型

**基本项**：基本分 = 品德总分 × 30% + 专业素质（加权平均分） × 70%

品德总分 = 评议成绩 × 70% + 记实成绩 × 30%
- 评议成绩 = 自评 × 5% + 学生代表评议 × 60% + 辅导员(班主任)评议 × 35%
- 记实成绩 = 基准 60 分 + 荣誉加分 - 处分扣分

**综合能力**：能力分 = 75 + 研究创新 × 30% + 专业技能 × 25% + 组织工作 × 15% + 体育美育 × 15% + 劳动教育和社会实践 × 15%

**计算引擎**：`backend/src/main/java/com/zjsu/scholarship/service/ScoreCalcService.java`
- 实现细则中的志愿服务时长换算、处分扣分映射、任职等级×类别得分、竞赛级别×奖项分值、A/B/C 类竞赛系数（1.2/1.0/0.5）、多人成果分摊系数表。
- 学生每提交一条材料即自动计算单项分；辅导员审核后触发整体重算。

### 2. 综合奖学金等级分配

优秀学生综合奖学金（一/二/三等）的等级**按综合能力排名先后确定**：

1. 管理员创建项目时配置每个等级的"比例%"（一等 3%、二等 6%、三等 12%）；
2. 学生完成基本项和综合能力填报后，辅导员审核通过，管理员一键"执行排名"；
3. 系统按**综合能力总分降序排序**，按比例切分名额；
4. 学生申报时校验硬性条件（均分≥75、体育≥80、劳动合格），查看"系统推荐等级"；
5. 辅导员审核确认最终授予等级。

源码：`RankingService.rankAndAssign(projectId)`

### 3. 角色与权限

- **学生**：填报综测、上传材料、查看推荐等级、申报奖学金
- **辅导员**：审核本院本届学生的材料和申请、批量通过
- **管理员**：学年管理、奖学金项目创建、规则配置、执行排名、发布公示

---

## 典型业务流程演示

> 全流程约 5 分钟，可还原完整评选场景：

1. **以 `admin` 登录** → 学年管理已有 2025-2026 学年。
2. **奖学金项目 → 新建项目**：
   - 类型选「优秀学生综合奖学金」
   - 等级：一等 3% / 2500 元，二等 6% / 1200 元，三等 12% / 800 元
   - 创建后状态自动为 `OPEN`
3. **退出，以学生 `20231001` 登录** → 基本项测评 →
   - 品德评议：添加自评、代表评议、辅导员评议（6 维度打分）
   - 品德记实：添加荣誉/志愿服务/处分记录
   - 综合能力：分别填报研究创新/专业技能/组织工作/体育美育/劳动实践
   - 点击「提交测评」
5. **以 `admin` 登录** → 奖学金项目 → 点击「执行排名与等级分配」：
   - 系统全员重算综测分 + 排序 + 按比例分配等级
   - 综测排名菜单可查看完整排名表
6. **以学生身份登录** → 奖学金申报 → 此时可看到「系统推荐等级」 → 点击申报。
7. **以辅导员身份登录** → 申请审核 → 批量通过。
8. **以 `admin` 登录** → 奖学金项目 → 点击「发布公示」。
9. **打开 http://localhost:5173/results**（无需登录）即可查看公示名单。

---

## 项目结构

```
project/
├── backend/                           # Spring Boot 后端
│   ├── pom.xml
│   ├── data/scholarship.mv.db         # H2 数据库（首次启动自动创建）
│   └── src/main/
│       ├── java/com/zjsu/scholarship/
│       │   ├── ScholarshipApplication.java
│       │   ├── common/                # R/Exception
│       │   ├── config/                # CORS/Web/Password
│       │   ├── security/              # JWT + 权限拦截
│       │   ├── entity/                # 12 个数据实体
│       │   ├── mapper/                # MyBatis-Plus
│       │   ├── service/               # AuthService / ScoreCalcService / EvaluationService / RankingService
│       │   └── controller/            # AuthController/StudentController/CounselorController/AdminController/PublicController
│       └── resources/
│           ├── application.yml
│           └── db/                    # schema.sql + data.sql
├── frontend/                          # React 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api.js                     # Axios + JWT 注入
│       ├── store.js                   # Zustand 全局认证
│       ├── App.jsx                    # 路由 + 角色守卫
│       ├── layouts/                   # 三个角色布局
│       └── pages/                     # Login / student/* / counselor/* / admin/* / ResultsPublic
├── start-backend.ps1
└── start-frontend.ps1
```

---

## API 概览

| 方法 | 路径 | 权限 |
|------|------|------|
| POST | `/api/auth/login` | 公开 |
| GET | `/api/auth/me` | 已登录 |
| POST | `/api/auth/change-password` | 已登录 |
| GET | `/api/student/me` | STUDENT |
| GET | `/api/student/evaluation/items` | STUDENT |
| POST | `/api/student/evaluation/moral-items` 等 | STUDENT |
| POST | `/api/student/evaluation/submit` | STUDENT |
| GET | `/api/student/scholarships/eligible` | STUDENT |
| POST | `/api/student/applications` | STUDENT |
| GET | `/api/counselor/students` | COUNSELOR/ADMIN |
| GET | `/api/counselor/items/pending` | COUNSELOR/ADMIN |
| POST | `/api/counselor/items/{kind}/{id}/review` | COUNSELOR/ADMIN |
| GET | `/api/counselor/applications` | COUNSELOR/ADMIN |
| POST | `/api/counselor/applications/{id}/review` | COUNSELOR/ADMIN |
| POST | `/api/counselor/applications/batch-review` | COUNSELOR/ADMIN |
| GET | `/api/admin/years` | ADMIN |
| POST | `/api/admin/projects` | ADMIN |
| POST | `/api/admin/projects/{id}/rank` | ADMIN |
| POST | `/api/admin/projects/{id}/publish` | ADMIN |
| GET | `/api/admin/ranking?yearId=` | ADMIN |
| GET | `/api/public/results?keyword=` | 公开 |

H2 控制台（可视化看数据）：**http://localhost:8080/h2**
- JDBC URL：`jdbc:h2:file:./data/scholarship`
- 用户名 `sa`，密码留空

---

## 常见问题

**Q：忘记密码怎么办？**
A：管理员可在数据库中清空 `users.password_hash` 字段，即可重新使用初始密码登录。

**Q：换其他数据库？**
A：在 `pom.xml` 加 mysql-connector 依赖，修改 `application.yml` 的 `datasource.url`/`driver-class-name` 即可。

**Q：如何重置数据？**
A：停止后端，删除 `backend/data/` 整个目录，重新启动即可。
