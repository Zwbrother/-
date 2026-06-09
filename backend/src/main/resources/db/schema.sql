-- ============================================
-- 浙江工商大学本科生奖学金评选系统 · 数据库 Schema
-- 依据《2025版学生素质评价办法》《2025版奖学金实施办法》
-- ============================================

-- ============= 用户与认证 =============
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(50) NOT NULL,
  role VARCHAR(20) NOT NULL,
  password_hash VARCHAR(120),
  email VARCHAR(100),
  phone VARCHAR(20),
  status VARCHAR(20) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS school_auth_mock (
  account VARCHAR(32) PRIMARY KEY,
  initial_password VARCHAR(100) NOT NULL
);

-- ============= 学生信息 =============
CREATE TABLE IF NOT EXISTS students (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  student_no VARCHAR(20) NOT NULL UNIQUE,
  name VARCHAR(50),
  gender VARCHAR(4),
  college VARCHAR(80),
  major VARCHAR(80),
  grade VARCHAR(10),
  class_name VARCHAR(50),
  dorm_no VARCHAR(20),
  -- 本科生特有字段
  cet4_score INT,
  cet6_score INT,
  pe_score DECIMAL(5,1),
  labor_evaluation VARCHAR(20) DEFAULT 'PENDING',
  -- 新增：体育免测
  pe_exempt BOOLEAN DEFAULT FALSE
);

-- ============= 学年 =============
CREATE TABLE IF NOT EXISTS academic_years (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  year_name VARCHAR(20) NOT NULL UNIQUE,
  start_date DATE,
  end_date DATE,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  fill_start_at TIMESTAMP NULL,
  fill_end_at TIMESTAMP NULL,
  review_start_at TIMESTAMP NULL,
  review_end_at TIMESTAMP NULL,
  public_start_at TIMESTAMP NULL,
  public_end_at TIMESTAMP NULL
);

-- ============= 综合测评记录（双轨制） =============
CREATE TABLE IF NOT EXISTS evaluation_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  academic_year_id BIGINT NOT NULL,
  -- 基本项：品德
  moral_appraisal_score DECIMAL(5,2) DEFAULT 0,
  moral_record_score DECIMAL(5,2) DEFAULT 0,
  moral_total DECIMAL(7,2) DEFAULT 0,
  -- 基本项：专业素质
  academic_weighted_avg DECIMAL(7,2) DEFAULT 0,
  -- 基本项总分 = 品德×30% + 专业×70%
  basic_total DECIMAL(8,3) DEFAULT 0,
  basic_rank INT,
  -- 综合能力 = 75 + Σ(五项×权重)
  ability_base INT DEFAULT 75,
  research_innovation DECIMAL(7,2) DEFAULT 0,
  professional_skill DECIMAL(7,2) DEFAULT 0,
  organization_work DECIMAL(7,2) DEFAULT 0,
  sports_aesthetics DECIMAL(7,2) DEFAULT 0,
  labor_practice DECIMAL(7,2) DEFAULT 0,
  ability_total DECIMAL(8,3) DEFAULT 0,
  ability_rank INT,
  -- 状态
  status VARCHAR(20) DEFAULT 'DRAFT',
  submitted_at TIMESTAMP NULL,
  CONSTRAINT uk_eval_ug UNIQUE (student_id, academic_year_id)
);

-- ============= 课程成绩 =============
CREATE TABLE IF NOT EXISTS course_grades (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  academic_year_id BIGINT NOT NULL,
  course_name VARCHAR(120),
  credit DECIMAL(4,1),
  score DECIMAL(5,1)
);

-- ============= 品德评议（6维度 × 3来源） =============
CREATE TABLE IF NOT EXISTS moral_appraisals (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  evaluation_id BIGINT NOT NULL,
  appraiser_type VARCHAR(20) NOT NULL,
  political_literacy DECIMAL(4,1) DEFAULT 0,
  legal_awareness DECIMAL(4,1) DEFAULT 0,
  mental_quality DECIMAL(4,1) DEFAULT 0,
  integrity_score DECIMAL(4,1) DEFAULT 0,
  teamwork DECIMAL(4,1) DEFAULT 0,
  social_responsibility DECIMAL(4,1) DEFAULT 0,
  total DECIMAL(5,2) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============= 品德记实项（保留原 moral_items 概念） =============
CREATE TABLE IF NOT EXISTS moral_record_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  evaluation_id BIGINT NOT NULL,
  item_type VARCHAR(30) NOT NULL,
  description VARCHAR(255),
  occurred_date DATE,
  hours DECIMAL(5,1),
  raw_value DECIMAL(7,2),
  honor_level VARCHAR(20),
  score DECIMAL(7,2) DEFAULT 0,
  attachment_url VARCHAR(255),
  review_status VARCHAR(20) DEFAULT 'PENDING',
  review_remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============= 综合能力-研究创新项 =============
CREATE TABLE IF NOT EXISTS research_innovation_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  evaluation_id BIGINT NOT NULL,
  item_type VARCHAR(30) NOT NULL,
  name VARCHAR(200),
  level_field VARCHAR(30),
  award_level VARCHAR(20),
  competition_category VARCHAR(4),
  patent_type VARCHAR(20),
  project_level VARCHAR(40),
  project_status VARCHAR(20),
  journal_level VARCHAR(50),
  total_authors INT DEFAULT 1,
  my_rank INT DEFAULT 1,
  has_advisor BOOLEAN DEFAULT FALSE,
  is_core_member BOOLEAN DEFAULT FALSE,
  description VARCHAR(255),
  occurred_date DATE,
  score DECIMAL(7,2) DEFAULT 0,
  attachment_url VARCHAR(255),
  review_status VARCHAR(20) DEFAULT 'PENDING',
  review_remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============= 综合能力-专业技能项 =============
CREATE TABLE IF NOT EXISTS professional_skill_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  evaluation_id BIGINT NOT NULL,
  item_type VARCHAR(30) NOT NULL,
  name VARCHAR(200),
  skill_category VARCHAR(50),
  skill_level VARCHAR(30),
  oral_exam_passed BOOLEAN DEFAULT FALSE,
  entrance_exam_result VARCHAR(20),
  description VARCHAR(255),
  occurred_date DATE,
  score DECIMAL(7,2) DEFAULT 0,
  attachment_url VARCHAR(255),
  review_status VARCHAR(20) DEFAULT 'PENDING',
  review_remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============= 综合能力-组织工作项 =============
CREATE TABLE IF NOT EXISTS organization_work_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  evaluation_id BIGINT NOT NULL,
  name VARCHAR(200),
  org_level VARCHAR(20),
  position_name VARCHAR(100),
  position_score INT DEFAULT 0,
  performance_grade VARCHAR(10),
  performance_score INT DEFAULT 0,
  duration_months INT,
  description VARCHAR(255),
  occurred_date DATE,
  score DECIMAL(7,2) DEFAULT 0,
  attachment_url VARCHAR(255),
  review_status VARCHAR(20) DEFAULT 'PENDING',
  review_remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============= 综合能力-体育美育项 =============
CREATE TABLE IF NOT EXISTS sports_aesthetics_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  evaluation_id BIGINT NOT NULL,
  item_type VARCHAR(30) NOT NULL,
  name VARCHAR(200),
  level_field VARCHAR(30),
  award_level VARCHAR(20),
  is_team BOOLEAN DEFAULT FALSE,
  is_core_member BOOLEAN DEFAULT FALSE,
  team_size INT DEFAULT 1,
  description VARCHAR(255),
  occurred_date DATE,
  score DECIMAL(7,2) DEFAULT 0,
  attachment_url VARCHAR(255),
  review_status VARCHAR(20) DEFAULT 'PENDING',
  review_remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============= 综合能力-劳动教育和社会实践项 =============
CREATE TABLE IF NOT EXISTS labor_practice_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  evaluation_id BIGINT NOT NULL,
  item_type VARCHAR(30) NOT NULL,
  name VARCHAR(200),
  level_field VARCHAR(30),
  award_level VARCHAR(20),
  is_team BOOLEAN DEFAULT FALSE,
  is_core_member BOOLEAN DEFAULT FALSE,
  team_size INT DEFAULT 1,
  description VARCHAR(255),
  occurred_date DATE,
  score DECIMAL(7,2) DEFAULT 0,
  attachment_url VARCHAR(255),
  review_status VARCHAR(20) DEFAULT 'PENDING',
  review_remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============= 奖学金项目 =============
CREATE TABLE IF NOT EXISTS scholarship_projects (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  academic_year_id BIGINT NOT NULL,
  type_code VARCHAR(40) NOT NULL,
  project_name VARCHAR(120) NOT NULL,
  description VARCHAR(500),
  apply_start_at TIMESTAMP NULL,
  apply_end_at TIMESTAMP NULL,
  status VARCHAR(20) DEFAULT 'DRAFT',
  ranked BOOLEAN DEFAULT FALSE,
  -- 申报硬性条件
  min_weighted_avg DECIMAL(5,1),
  min_pe_score DECIMAL(5,1),
  need_labor_pass BOOLEAN DEFAULT TRUE,
  foreign_lang_requirement VARCHAR(200),
  no_discipline BOOLEAN DEFAULT TRUE,
  remark VARCHAR(500),
  -- 新增：结构化外语条件
  foreign_lang_avg_min DECIMAL(5,1),
  foreign_lang_avg_first DECIMAL(5,1),
  require_cet4_pass BOOLEAN DEFAULT FALSE,
  -- 新增：排名过滤
  rank_basic_max_ratio DECIMAL(5,2),
  rank_ability_first DECIMAL(5,2),
  rank_basic_first DECIMAL(5,2)
);

CREATE TABLE IF NOT EXISTS scholarship_levels (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  level_name VARCHAR(50) NOT NULL,
  level_order INT NOT NULL,
  ratio DECIMAL(5,2),
  amount DECIMAL(10,2),
  quota INT,
  -- 新增：双轨制排名条件
  rank_basic_max_ratio DECIMAL(5,2),
  rank_ability_max_ratio DECIMAL(5,2)
);

CREATE TABLE IF NOT EXISTS scholarship_criteria (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  rule_type VARCHAR(40) NOT NULL,
  rule_value VARCHAR(200)
);

-- ============= 单项奖学金定义（学院自定义） =============
CREATE TABLE IF NOT EXISTS special_scholarships (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  academic_year_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(500),
  amount DECIMAL(10,2),
  quota INT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- ============= 申请 =============
CREATE TABLE IF NOT EXISTS applications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  evaluation_id BIGINT,
  -- 快照
  snapshot_basic_total DECIMAL(8,3),
  snapshot_basic_rank INT,
  snapshot_ability_total DECIMAL(8,3),
  snapshot_ability_rank INT,
  -- 系统推荐 / 最终授予
  auto_level_id BIGINT,
  final_level_id BIGINT,
  status VARCHAR(20) DEFAULT 'SUBMITTED',
  reject_reason VARCHAR(255),
  submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  reviewed_at TIMESTAMP NULL,
  reviewer_id BIGINT,
  -- 新增：申报分类限制
  application_category VARCHAR(30),
  CONSTRAINT uk_app_ug UNIQUE (student_id, project_id)
);

-- ============= 审计日志 =============
CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_account VARCHAR(32),
  action VARCHAR(100),
  detail VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============= 学生代表 =============
CREATE TABLE IF NOT EXISTS student_representatives (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  academic_year_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  class_name VARCHAR(50),
  elected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (academic_year_id, student_id)
);

-- ============= 申诉记录 =============
CREATE TABLE IF NOT EXISTS appeal_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  application_id BIGINT,
  student_id BIGINT NOT NULL,
  project_id BIGINT,
  appeal_level VARCHAR(20),
  reason VARCHAR(1000),
  status VARCHAR(20) DEFAULT 'PENDING',
  response VARCHAR(1000),
  submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  responded_at TIMESTAMP
);

-- ============= 考研奖学金申报 =============
CREATE TABLE IF NOT EXISTS graduate_exam_applications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  academic_year_id BIGINT NOT NULL,
  exam_type VARCHAR(20),
  has_interview_qualification BOOLEAN DEFAULT FALSE,
  is_admitted BOOLEAN DEFAULT FALSE,
  school_name VARCHAR(200),
  major_name VARCHAR(200),
  status VARCHAR(20) DEFAULT 'SUBMITTED',
  final_level VARCHAR(20),
  reject_reason VARCHAR(500),
  attachment_url VARCHAR(255),
  submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (student_id, academic_year_id)
);

-- ============= 处分记录（追踪处分状态） =============
CREATE TABLE IF NOT EXISTS discipline_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  discipline_type VARCHAR(30),
  description VARCHAR(500),
  occurred_date DATE,
  is_resolved BOOLEAN DEFAULT FALSE,
  resolved_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============= 学院级配置 =============
CREATE TABLE IF NOT EXISTS college_configs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  college_name VARCHAR(80) NOT NULL,
  config_key VARCHAR(100) NOT NULL,
  config_value VARCHAR(500),
  description VARCHAR(255),
  UNIQUE (college_name, config_key)
);

-- ============================================================
--  数据库迁移（对已存在数据库增量添加字段）
-- ============================================================
ALTER TABLE scholarship_projects ADD COLUMN IF NOT EXISTS foreign_lang_avg_min DECIMAL(5,1);
ALTER TABLE scholarship_projects ADD COLUMN IF NOT EXISTS foreign_lang_avg_first DECIMAL(5,1);
ALTER TABLE scholarship_projects ADD COLUMN IF NOT EXISTS require_cet4_pass BOOLEAN DEFAULT FALSE;
ALTER TABLE scholarship_projects ADD COLUMN IF NOT EXISTS rank_basic_max_ratio DECIMAL(5,2);
ALTER TABLE scholarship_projects ADD COLUMN IF NOT EXISTS rank_ability_first DECIMAL(5,2);
ALTER TABLE scholarship_projects ADD COLUMN IF NOT EXISTS rank_basic_first DECIMAL(5,2);
ALTER TABLE scholarship_levels ADD COLUMN IF NOT EXISTS rank_basic_max_ratio DECIMAL(5,2);
ALTER TABLE scholarship_levels ADD COLUMN IF NOT EXISTS rank_ability_max_ratio DECIMAL(5,2);
ALTER TABLE students ADD COLUMN IF NOT EXISTS pe_exempt BOOLEAN DEFAULT FALSE;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS application_category VARCHAR(30);
ALTER TABLE moral_record_items ADD COLUMN IF NOT EXISTS honor_level VARCHAR(20);
ALTER TABLE professional_skill_items ADD COLUMN IF NOT EXISTS oral_exam_passed BOOLEAN DEFAULT FALSE;
ALTER TABLE professional_skill_items ADD COLUMN IF NOT EXISTS entrance_exam_result VARCHAR(20);
ALTER TABLE graduate_exam_applications ADD COLUMN IF NOT EXISTS attachment_url VARCHAR(255);
ALTER TABLE graduate_exam_applications ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(500);
ALTER TABLE graduate_exam_applications ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP NULL;
ALTER TABLE graduate_exam_applications ADD COLUMN IF NOT EXISTS reviewer_id BIGINT;
