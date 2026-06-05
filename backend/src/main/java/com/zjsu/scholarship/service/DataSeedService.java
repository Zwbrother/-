package com.zjsu.scholarship.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zjsu.scholarship.entity.*;
import com.zjsu.scholarship.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** 本科生演示数据播种 */
@Service
public class DataSeedService {

    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final AcademicYearMapper yearMapper;
    private final CourseGradeMapper courseGradeMapper;
    private final EvaluationRecordMapper evalMapper;
    private final MoralAppraisalMapper moralAppraisalMapper;
    private final MoralRecordItemMapper moralRecordMapper;
    private final ResearchInnovationItemMapper riMapper;
    private final ProfessionalSkillItemMapper psMapper;
    private final OrganizationWorkItemMapper owMapper;
    private final ScholarshipProjectMapper projectMapper;
    private final ApplicationMapper applicationMapper;
    private final EvaluationService evaluationService;

    private final String[] MAJORS = {"人工智能", "通信工程", "电子信息工程"};
    private final String[] COURSES = {"高等数学", "线性代数", "C语言程序设计", "大学英语"};
    private final Random rand = new Random(42L);

    public DataSeedService(UserMapper userMapper, StudentMapper studentMapper,
                           AcademicYearMapper yearMapper, CourseGradeMapper courseGradeMapper,
                           EvaluationRecordMapper evalMapper,
                           MoralAppraisalMapper moralAppraisalMapper,
                           MoralRecordItemMapper moralRecordMapper,
                           ResearchInnovationItemMapper riMapper,
                           ProfessionalSkillItemMapper psMapper,
                           OrganizationWorkItemMapper owMapper,
                           ScholarshipProjectMapper projectMapper,
                           ApplicationMapper applicationMapper,
                           EvaluationService evaluationService) {
        this.userMapper = userMapper;
        this.studentMapper = studentMapper;
        this.yearMapper = yearMapper;
        this.courseGradeMapper = courseGradeMapper;
        this.evalMapper = evalMapper;
        this.moralAppraisalMapper = moralAppraisalMapper;
        this.moralRecordMapper = moralRecordMapper;
        this.riMapper = riMapper;
        this.psMapper = psMapper;
        this.owMapper = owMapper;
        this.projectMapper = projectMapper;
        this.applicationMapper = applicationMapper;
        this.evaluationService = evaluationService;
    }

    @Transactional
    public Map<String, Object> seed() {
        AcademicYear year = yearMapper.selectOne(Wrappers.<AcademicYear>lambdaQuery()
                .eq(AcademicYear::getStatus, "ACTIVE").last("LIMIT 1"));
        if (year == null) throw new RuntimeException("未找到有效学年，请先创建学年");

        ScholarshipProject project = projectMapper.selectOne(
                Wrappers.<ScholarshipProject>lambdaQuery()
                        .eq(ScholarshipProject::getAcademicYearId, year.getId()).last("LIMIT 1"));
        if (project == null) throw new RuntimeException("未找到奖学金项目，请先在管理员端创建项目");

        int created = 0, skipped = 0;
        for (int i = 0; i < 50; i++) {
            String major = MAJORS[i % 3];
            String studentNo = String.format("20235%03d", i + 20);

            if (studentMapper.selectOne(Wrappers.<Student>lambdaQuery()
                    .eq(Student::getStudentNo, studentNo)) != null) { skipped++; continue; }

            String name = randomName();
            String gender = (i % 3 == 0) ? "女" : "男";
            String grade = i < 25 ? "大二" : "大一";
            String className = major.substring(0, 2) + (i < 25 ? "2301" : "2401");

            // User
            User user = new User();
            user.setAccount(studentNo); user.setName(name);
            user.setRole("STUDENT"); user.setStatus("ACTIVE"); user.setCreatedAt(LocalDateTime.now());
            userMapper.insert(user);

            // Student
            Student stu = new Student();
            stu.setUserId(user.getId()); stu.setStudentNo(studentNo); stu.setName(name);
            stu.setGender(gender); stu.setCollege("信息与电子工程学院");
            stu.setMajor(major); stu.setGrade(grade); stu.setClassName(className);
            stu.setDormNo("D" + (rand.nextInt(5) + 1) + "-" + String.format("%03d", rand.nextInt(400) + 100));
            stu.setCet4Score(rand.nextInt(200) + 400);
            stu.setCet6Score(rand.nextDouble() < 0.3 ? rand.nextInt(100) + 425 : 0);
            stu.setPeScore(new BigDecimal(String.format("%.1f", 70 + rand.nextDouble() * 25)));
            stu.setLaborEvaluation(rand.nextDouble() < 0.9 ? "PASS" : "PENDING");
            studentMapper.insert(stu);

            // 课程
            for (String cn : COURSES) {
                CourseGrade cg = new CourseGrade();
                cg.setStudentId(stu.getId()); cg.setAcademicYearId(year.getId());
                cg.setCourseName(cn); cg.setCredit(new BigDecimal("3.0"));
                cg.setScore(new BigDecimal(String.format("%.1f", 65 + rand.nextDouble() * 30)));
                courseGradeMapper.insert(cg);
            }

            // 评价记录
            EvaluationRecord rec = evaluationService.findOrCreate(stu.getId(), year.getId());

            // 品德评议
            for (String at : new String[]{"SELF", "STUDENT_REP", "COUNSELOR"}) {
                MoralAppraisal appr = new MoralAppraisal();
                appr.setEvaluationId(rec.getId()); appr.setAppraiserType(at);
                appr.setPoliticalLiteracy(randBd(12, 20));
                appr.setLegalAwareness(randBd(12, 20));
                appr.setMentalQuality(randBd(12, 20));
                appr.setIntegrityScore(randBd(12, 20));
                appr.setTeamwork(randBd(12, 20));
                appr.setSocialResponsibility(randBd(12, 20));
                moralAppraisalMapper.insert(appr);
            }

            // 品德记实
            if (rand.nextDouble() < 0.5) {
                MoralRecordItem mi = new MoralRecordItem();
                mi.setEvaluationId(rec.getId()); mi.setItemType("HONOR");
                mi.setDescription(rand.nextBoolean() ? "SCHOOL" : "COLLEGE");
                mi.setReviewStatus("APPROVED"); mi.setCreatedAt(LocalDateTime.now());
                mi.setScore(rand.nextBoolean() ? new BigDecimal("8") : new BigDecimal("5"));
                moralRecordMapper.insert(mi);
            }

            // 研究创新
            if (rand.nextDouble() < 0.6) {
                ResearchInnovationItem ri = new ResearchInnovationItem();
                ri.setEvaluationId(rec.getId()); ri.setItemType("COMPETITION");
                ri.setName("数学建模竞赛"); ri.setLevelField("SCHOOL"); ri.setAwardLevel("SECOND");
                ri.setReviewStatus("APPROVED"); ri.setCreatedAt(LocalDateTime.now()); ri.setScore(new BigDecimal("8"));
                riMapper.insert(ri);
            }

            // 组织工作
            if (rand.nextDouble() < 0.4) {
                OrganizationWorkItem ow = new OrganizationWorkItem();
                ow.setEvaluationId(rec.getId()); ow.setName("班干部");
                ow.setOrgLevel("CLASS"); ow.setPositionScore(12);
                ow.setPerformanceGrade("EXCELLENT"); ow.setDurationMonths(12);
                ow.setReviewStatus("APPROVED"); ow.setCreatedAt(LocalDateTime.now());
                ow.setScore(new BigDecimal("20"));
                owMapper.insert(ow);
            }

            evaluationService.recalculateAll(stu.getId(), year.getId());
            evaluationService.submit(stu.getId(), year.getId());

            // 申请
            if (applicationMapper.selectOne(Wrappers.<Application>lambdaQuery()
                    .eq(Application::getStudentId, stu.getId())
                    .eq(Application::getProjectId, project.getId())) == null) {
                EvaluationRecord finalRec = evaluationService.findOrCreate(stu.getId(), year.getId());
                Application app = new Application();
                app.setStudentId(stu.getId()); app.setProjectId(project.getId());
                app.setEvaluationId(finalRec.getId());
                app.setSnapshotBasicTotal(finalRec.getBasicTotal());
                app.setSnapshotAbilityTotal(finalRec.getAbilityTotal());
                app.setStatus("SUBMITTED"); app.setSubmittedAt(LocalDateTime.now());
                applicationMapper.insert(app);
            }
            created++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created); result.put("skipped", skipped);
        result.put("year", year.getYearName());
        result.put("project", project == null ? "无" : project.getProjectName());
        return result;
    }

    private BigDecimal randBd(int min, int max) {
        return BigDecimal.valueOf(min + rand.nextInt(max - min + 1));
    }

    private String randomName() {
        String[] surnames = {"张","李","王","赵","钱","孙","周","吴","郑","冯","陈","褚","卫","蒋","沈","韩","杨"};
        String[] givens = {"伟","芳","秀英","敏","静","丽","强","磊","军","洋","勇","艳","杰","娜","超","明","涛","鑫"};
        return surnames[rand.nextInt(surnames.length)] + givens[rand.nextInt(givens.length)];
    }
}
