package com.zjsu.scholarship.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zjsu.scholarship.common.BusinessException;
import com.zjsu.scholarship.common.R;
import com.zjsu.scholarship.entity.*;
import com.zjsu.scholarship.mapper.*;
import com.zjsu.scholarship.security.RequireRole;
import com.zjsu.scholarship.service.DataSeedService;
import com.zjsu.scholarship.service.ImportService;
import com.zjsu.scholarship.service.RankingService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequireRole({"ADMIN", "COUNSELOR"})
public class AdminController {

    private final AcademicYearMapper yearMapper;
    private final ScholarshipProjectMapper projectMapper;
    private final ScholarshipLevelMapper levelMapper;
    private final ScholarshipCriterionMapper criterionMapper;
    private final ApplicationMapper applicationMapper;
    private final StudentMapper studentMapper;
    private final EvaluationRecordMapper evalMapper;
    private final StudentRepresentativeMapper representativeMapper;
    private final DisciplineRecordMapper disciplineMapper;
    private final AppealRecordMapper appealMapper;
    private final RankingService rankingService;
    private final ImportService importService;
    private final DataSeedService dataSeedService;

    public AdminController(AcademicYearMapper yearMapper, ScholarshipProjectMapper projectMapper,
                           ScholarshipLevelMapper levelMapper, ScholarshipCriterionMapper criterionMapper,
                           ApplicationMapper applicationMapper, StudentMapper studentMapper,
                           EvaluationRecordMapper evalMapper,
                           StudentRepresentativeMapper representativeMapper,
                           DisciplineRecordMapper disciplineMapper,
                           AppealRecordMapper appealMapper,
                           RankingService rankingService,
                           ImportService importService, DataSeedService dataSeedService) {
        this.yearMapper = yearMapper;
        this.projectMapper = projectMapper;
        this.levelMapper = levelMapper;
        this.criterionMapper = criterionMapper;
        this.applicationMapper = applicationMapper;
        this.studentMapper = studentMapper;
        this.evalMapper = evalMapper;
        this.representativeMapper = representativeMapper;
        this.disciplineMapper = disciplineMapper;
        this.appealMapper = appealMapper;
        this.rankingService = rankingService;
        this.importService = importService;
        this.dataSeedService = dataSeedService;
    }

    // ============ 学年 ============
    @GetMapping("/years")
    public R<List<AcademicYear>> years() {
        return R.ok(yearMapper.selectList(Wrappers.<AcademicYear>lambdaQuery()
                .orderByDesc(AcademicYear::getId)));
    }

    @PostMapping("/years")
    public R<AcademicYear> createYear(@RequestBody AcademicYear year) {
        year.setId(null);
        if (year.getStatus() == null) year.setStatus("ACTIVE");
        yearMapper.insert(year);
        return R.ok(year);
    }

    // ============ 项目 ============
    @GetMapping("/projects")
    public R<List<Map<String, Object>>> projects(@RequestParam(required = false) Long yearId) {
        List<ScholarshipProject> ps = projectMapper.selectList(Wrappers.<ScholarshipProject>lambdaQuery()
                .eq(yearId != null, ScholarshipProject::getAcademicYearId, yearId)
                .orderByDesc(ScholarshipProject::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ScholarshipProject p : ps) {
            Map<String, Object> m = new HashMap<>();
            m.put("project", p);
            m.put("levels", levelMapper.selectList(Wrappers.<ScholarshipLevel>lambdaQuery()
                    .eq(ScholarshipLevel::getProjectId, p.getId())
                    .orderByAsc(ScholarshipLevel::getLevelOrder)));
            m.put("criteria", criterionMapper.selectList(Wrappers.<ScholarshipCriterion>lambdaQuery()
                    .eq(ScholarshipCriterion::getProjectId, p.getId())));
            result.add(m);
        }
        return R.ok(result);
    }

    @PostMapping("/projects")
    public R<ScholarshipProject> createProject(@RequestBody Map<String, Object> body) {
        ScholarshipProject p = buildProjectFromBody(body);
        p.setStatus("OPEN");
        p.setRanked(false);
        projectMapper.insert(p);
        saveLevelsAndCriteria(p.getId(), body);
        return R.ok(p);
    }

    @PutMapping("/projects/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ScholarshipProject p = projectMapper.selectById(id);
        if (p == null) throw new BusinessException("项目不存在");
        p.setStatus(body.get("status"));
        projectMapper.updateById(p);
        return R.ok();
    }

    @PutMapping("/projects/{id}")
    @RequireRole("ADMIN")
    public R<ScholarshipProject> updateProject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ScholarshipProject p = projectMapper.selectById(id);
        if (p == null) throw new BusinessException("项目不存在");
        if (body.containsKey("projectName")) p.setProjectName((String) body.get("projectName"));
        if (body.containsKey("description")) p.setDescription((String) body.get("description"));
        if (body.containsKey("typeCode")) p.setTypeCode((String) body.get("typeCode"));
        if (body.containsKey("foreignLangAvgMin") && body.get("foreignLangAvgMin") != null)
            p.setForeignLangAvgMin(new BigDecimal(body.get("foreignLangAvgMin").toString()));
        if (body.containsKey("foreignLangAvgFirst") && body.get("foreignLangAvgFirst") != null)
            p.setForeignLangAvgFirst(new BigDecimal(body.get("foreignLangAvgFirst").toString()));
        if (body.containsKey("requireCet4Pass"))
            p.setRequireCet4Pass((Boolean) body.get("requireCet4Pass"));
        if (body.containsKey("rankBasicMaxRatio") && body.get("rankBasicMaxRatio") != null)
            p.setRankBasicMaxRatio(new BigDecimal(body.get("rankBasicMaxRatio").toString()));
        if (body.containsKey("rankAbilityFirst") && body.get("rankAbilityFirst") != null)
            p.setRankAbilityFirst(new BigDecimal(body.get("rankAbilityFirst").toString()));
        if (body.containsKey("rankBasicFirst") && body.get("rankBasicFirst") != null)
            p.setRankBasicFirst(new BigDecimal(body.get("rankBasicFirst").toString()));
        projectMapper.updateById(p);
        // 重新保存等级和条件
        levelMapper.delete(Wrappers.<ScholarshipLevel>lambdaQuery().eq(ScholarshipLevel::getProjectId, id));
        criterionMapper.delete(Wrappers.<ScholarshipCriterion>lambdaQuery().eq(ScholarshipCriterion::getProjectId, id));
        saveLevelsAndCriteria(id, body);
        return R.ok(p);
    }

    @DeleteMapping("/projects/{id}")
    public R<Void> deleteProject(@PathVariable Long id) {
        ScholarshipProject p = projectMapper.selectById(id);
        if (p == null) return R.ok();
        levelMapper.delete(Wrappers.<ScholarshipLevel>lambdaQuery().eq(ScholarshipLevel::getProjectId, id));
        criterionMapper.delete(Wrappers.<ScholarshipCriterion>lambdaQuery().eq(ScholarshipCriterion::getProjectId, id));
        applicationMapper.delete(Wrappers.<Application>lambdaQuery().eq(Application::getProjectId, id));
        projectMapper.deleteById(id);
        return R.ok();
    }

    @PostMapping("/projects/{id}/rank")
    public R<Map<String, Object>> rank(@PathVariable Long id) {
        return R.ok(rankingService.rankAndAssign(id));
    }

    @PostMapping("/projects/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        ScholarshipProject p = projectMapper.selectById(id);
        if (p == null) throw new BusinessException("项目不存在");
        p.setStatus("PUBLISHED");
        projectMapper.updateById(p);
        List<Application> apps = applicationMapper.selectList(Wrappers.<Application>lambdaQuery()
                .eq(Application::getProjectId, id)
                .eq(Application::getStatus, "APPROVED"));
        for (Application a : apps) {
            a.setStatus("PUBLISHED");
            applicationMapper.updateById(a);
        }
        return R.ok();
    }

    // ============ 看板 ============
    @GetMapping("/stats/dashboard")
    public R<Map<String, Object>> dashboard() {
        Map<String, Object> m = new HashMap<>();
        m.put("students", studentMapper.selectCount(null));
        m.put("projects", projectMapper.selectCount(null));
        m.put("applicationsTotal", applicationMapper.selectCount(null));
        m.put("applicationsApproved", applicationMapper.selectCount(
                Wrappers.<Application>lambdaQuery().in(Application::getStatus, Arrays.asList("APPROVED", "PUBLISHED"))));
        m.put("applicationsPending", applicationMapper.selectCount(
                Wrappers.<Application>lambdaQuery().eq(Application::getStatus, "SUBMITTED")));
        m.put("evaluations", evalMapper.selectCount(null));
        return R.ok(m);
    }

    // ============ 排名 ============
    @GetMapping("/ranking")
    public R<List<Map<String, Object>>> ranking(@RequestParam Long yearId,
                                                 @RequestParam(required = false) String major) {
        List<EvaluationRecord> recs = evalMapper.selectList(Wrappers.<EvaluationRecord>lambdaQuery()
                .eq(EvaluationRecord::getAcademicYearId, yearId)
                .orderByAsc(EvaluationRecord::getBasicRank));
        List<Map<String, Object>> result = new ArrayList<>();
        for (EvaluationRecord r : recs) {
            Student stu = studentMapper.selectById(r.getStudentId());
            if (major != null && !major.isEmpty() && (stu == null || !major.equals(stu.getMajor()))) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("evaluation", r);
            m.put("student", stu);
            result.add(m);
        }
        return R.ok(result);
    }

    // ============ 获奖名单预览 ============
    @GetMapping("/projects/{id}/award-preview")
    public R<Map<String, Object>> awardPreview(@PathVariable Long id) {
        ScholarshipProject project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException("项目不存在");
        List<ScholarshipLevel> levels = levelMapper.selectList(Wrappers.<ScholarshipLevel>lambdaQuery()
                .eq(ScholarshipLevel::getProjectId, id).orderByAsc(ScholarshipLevel::getLevelOrder));
        List<Application> apps = applicationMapper.selectList(Wrappers.<Application>lambdaQuery()
                .eq(Application::getProjectId, id));
        int totalQuota = levels.stream().mapToInt(l -> l.getQuota() == null ? 0 : l.getQuota()).sum();

        List<Map<String, Object>> levelGroups = new ArrayList<>();
        for (ScholarshipLevel lvl : levels) {
            List<Map<String, Object>> members = new ArrayList<>();
            for (Application app : apps) {
                if (lvl.getId().equals(app.getAutoLevelId())) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("student", studentMapper.selectById(app.getStudentId()));
                    row.put("application", app);
                    row.put("evaluation", evalMapper.selectOne(Wrappers.<EvaluationRecord>lambdaQuery()
                            .eq(EvaluationRecord::getStudentId, app.getStudentId())
                            .eq(EvaluationRecord::getAcademicYearId, project.getAcademicYearId())));
                    members.add(row);
                }
            }
            members.sort(Comparator.comparing(a -> ((Application) a.get("application")).getSnapshotAbilityRank(),
                    Comparator.nullsLast(Comparator.naturalOrder())));
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("level", lvl);
            group.put("members", members);
            levelGroups.add(group);
        }

        List<Map<String, Object>> unassigned = new ArrayList<>();
        for (Application app : apps) {
            if (app.getAutoLevelId() == null) {
                Map<String, Object> row = new LinkedHashMap<>();
                Student stu = studentMapper.selectById(app.getStudentId());
                row.put("student", stu);
                row.put("application", app);
                row.put("evaluation", evalMapper.selectOne(Wrappers.<EvaluationRecord>lambdaQuery()
                        .eq(EvaluationRecord::getStudentId, app.getStudentId())
                        .eq(EvaluationRecord::getAcademicYearId, project.getAcademicYearId())));
                row.put("reason", buildUnassignedReason(app, stu, totalQuota));
                unassigned.add(row);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project", project);
        result.put("levelGroups", levelGroups);
        result.put("unassigned", unassigned);
        result.put("totalApplicants", apps.size());
        return R.ok(result);
    }

    // ============ 导出 ============
    @GetMapping("/export/accounts")
    public void exportAccounts(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''student_accounts.csv");
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        List<Student> students = studentMapper.selectList(Wrappers.<Student>lambdaQuery().orderByAsc(Student::getStudentNo));
        StringBuilder sb = new StringBuilder();
        sb.append("学号,姓名,专业,年级,班级,初始密码\r\n");
        for (Student s : students) {
            sb.append(s.getStudentNo()).append(",").append(s.getName()).append(",")
              .append(s.getMajor()).append(",").append(s.getGrade()).append(",")
              .append(s.getClassName()).append(",").append("123456").append("\r\n");
        }
        response.getOutputStream().write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ============ 演示数据 ============
    @PostMapping("/seed-demo")
    @RequireRole("ADMIN")
    public R<Map<String, Object>> seedDemo() {
        return R.ok(dataSeedService.seed());
    }

    // ============ 批量导入 ============
    @GetMapping("/import/template/students")
    public void templateStudents(HttpServletResponse response) throws Exception {
        importService.generateStudentTemplate(response);
    }

    @GetMapping("/import/template/grades")
    public void templateGrades(HttpServletResponse response) throws Exception {
        importService.generateGradeTemplate(response);
    }

    @PostMapping("/import/students")
    @RequireRole("ADMIN")
    public R<Map<String, Object>> importStudents(@RequestParam("file") MultipartFile file) {
        return R.ok(importService.importStudents(file));
    }

    @PostMapping("/import/grades")
    @RequireRole("ADMIN")
    public R<Map<String, Object>> importGrades(@RequestParam("file") MultipartFile file,
                                                @RequestParam Long yearId) {
        return R.ok(importService.importGrades(file, yearId));
    }

    // ============ 学生代表管理 ============

    @GetMapping("/representatives")
    public R<List<Map<String, Object>>> representatives(@RequestParam(required = false) Long yearId) {
        List<StudentRepresentative> reps = representativeMapper.selectList(
                Wrappers.<StudentRepresentative>lambdaQuery()
                        .eq(yearId != null, StudentRepresentative::getAcademicYearId, yearId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentRepresentative rep : reps) {
            Map<String, Object> m = new HashMap<>();
            m.put("rep", rep);
            m.put("student", studentMapper.selectById(rep.getStudentId()));
            result.add(m);
        }
        return R.ok(result);
    }

    @PostMapping("/representatives")
    @RequireRole("ADMIN")
    public R<StudentRepresentative> addRepresentative(@RequestBody Map<String, Object> body) {
        Long studentId = toLong(body.get("studentId"));
        Long yearId = toLong(body.get("academicYearId"));
        // 检查是否已存在
        StudentRepresentative exist = representativeMapper.selectOne(
                Wrappers.<StudentRepresentative>lambdaQuery()
                        .eq(StudentRepresentative::getAcademicYearId, yearId)
                        .eq(StudentRepresentative::getStudentId, studentId));
        if (exist != null) throw new BusinessException("该学生已是本学年代表");

        StudentRepresentative rep = new StudentRepresentative();
        rep.setAcademicYearId(yearId);
        rep.setStudentId(studentId);
        rep.setClassName((String) body.get("className"));
        rep.setElectedAt(java.time.LocalDateTime.now());
        representativeMapper.insert(rep);
        return R.ok(rep);
    }

    @DeleteMapping("/representatives/{id}")
    @RequireRole("ADMIN")
    public R<Void> removeRepresentative(@PathVariable Long id) {
        representativeMapper.deleteById(id);
        return R.ok();
    }

    @GetMapping("/representatives/check-ratio")
    public R<Map<String, Object>> checkRepresentativeRatio(@RequestParam Long yearId) {
        long totalStudents = studentMapper.selectCount(null);
        long totalReps = representativeMapper.selectCount(
                Wrappers.<StudentRepresentative>lambdaQuery()
                        .eq(StudentRepresentative::getAcademicYearId, yearId));
        BigDecimal ratio = totalStudents > 0
                ? BigDecimal.valueOf(totalReps).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalStudents), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Map<String, Object> result = new HashMap<>();
        result.put("totalStudents", totalStudents);
        result.put("totalReps", totalReps);
        result.put("ratio", ratio);
        result.put("meetsRequirement", ratio.compareTo(new BigDecimal("30")) >= 0);
        return R.ok(result);
    }

    // ============ 处分管理 ============

    @GetMapping("/discipline")
    public R<List<Map<String, Object>>> disciplineRecords(@RequestParam(required = false) Long studentId) {
        List<DisciplineRecord> records = disciplineMapper.selectList(
                Wrappers.<DisciplineRecord>lambdaQuery()
                        .eq(studentId != null, DisciplineRecord::getStudentId, studentId)
                        .orderByDesc(DisciplineRecord::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (DisciplineRecord r : records) {
            Map<String, Object> m = new HashMap<>();
            m.put("record", r);
            m.put("student", studentMapper.selectById(r.getStudentId()));
            result.add(m);
        }
        return R.ok(result);
    }

    @PostMapping("/discipline")
    @RequireRole("ADMIN")
    public R<DisciplineRecord> addDiscipline(@RequestBody DisciplineRecord record) {
        record.setId(null);
        record.setIsResolved(false);
        record.setCreatedAt(java.time.LocalDateTime.now());
        disciplineMapper.insert(record);
        return R.ok(record);
    }

    @PutMapping("/discipline/{id}/resolve")
    @RequireRole("ADMIN")
    public R<DisciplineRecord> resolveDiscipline(@PathVariable Long id) {
        DisciplineRecord r = disciplineMapper.selectById(id);
        if (r == null) throw new BusinessException("记录不存在");
        r.setIsResolved(true);
        r.setResolvedAt(java.time.LocalDateTime.now());
        disciplineMapper.updateById(r);
        return R.ok(r);
    }

    @DeleteMapping("/discipline/{id}")
    @RequireRole("ADMIN")
    public R<Void> deleteDiscipline(@PathVariable Long id) {
        disciplineMapper.deleteById(id);
        return R.ok();
    }

    // ============ 申诉管理 ============

    @GetMapping("/appeals")
    public R<List<Map<String, Object>>> appeals(@RequestParam(required = false) String status) {
        List<AppealRecord> records = appealMapper.selectList(
                Wrappers.<AppealRecord>lambdaQuery()
                        .eq(status != null && !status.isEmpty(), AppealRecord::getStatus, status)
                        .orderByDesc(AppealRecord::getSubmittedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AppealRecord r : records) {
            Map<String, Object> m = new HashMap<>();
            m.put("appeal", r);
            m.put("student", studentMapper.selectById(r.getStudentId()));
            if (r.getProjectId() != null) m.put("project", projectMapper.selectById(r.getProjectId()));
            result.add(m);
        }
        return R.ok(result);
    }

    @PutMapping("/appeals/{id}/process")
    public R<AppealRecord> processAppeal(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AppealRecord r = appealMapper.selectById(id);
        if (r == null) throw new BusinessException("申诉不存在");
        r.setStatus(body.get("status"));
        r.setResponse(body.get("response"));
        r.setRespondedAt(java.time.LocalDateTime.now());
        appealMapper.updateById(r);
        return R.ok(r);
    }

    // ============ 辅助 ============
    private ScholarshipProject buildProjectFromBody(Map<String, Object> body) {
        ScholarshipProject p = new ScholarshipProject();
        p.setAcademicYearId(toLong(body.get("academicYearId")));
        p.setTypeCode((String) body.get("typeCode"));
        p.setProjectName((String) body.get("projectName"));
        p.setDescription((String) body.get("description"));
        if (body.containsKey("minWeightedAvg") && body.get("minWeightedAvg") != null)
            p.setMinWeightedAvg(new BigDecimal(body.get("minWeightedAvg").toString()));
        if (body.containsKey("minPeScore") && body.get("minPeScore") != null)
            p.setMinPeScore(new BigDecimal(body.get("minPeScore").toString()));
        if (body.containsKey("needLaborPass"))
            p.setNeedLaborPass((Boolean) body.get("needLaborPass"));
        p.setForeignLangRequirement((String) body.get("foreignLangRequirement"));
        if (body.containsKey("noDiscipline"))
            p.setNoDiscipline((Boolean) body.get("noDiscipline"));
        if (body.containsKey("foreignLangAvgMin") && body.get("foreignLangAvgMin") != null)
            p.setForeignLangAvgMin(new BigDecimal(body.get("foreignLangAvgMin").toString()));
        if (body.containsKey("foreignLangAvgFirst") && body.get("foreignLangAvgFirst") != null)
            p.setForeignLangAvgFirst(new BigDecimal(body.get("foreignLangAvgFirst").toString()));
        if (body.containsKey("requireCet4Pass"))
            p.setRequireCet4Pass((Boolean) body.get("requireCet4Pass"));
        if (body.containsKey("rankBasicMaxRatio") && body.get("rankBasicMaxRatio") != null)
            p.setRankBasicMaxRatio(new BigDecimal(body.get("rankBasicMaxRatio").toString()));
        if (body.containsKey("rankAbilityFirst") && body.get("rankAbilityFirst") != null)
            p.setRankAbilityFirst(new BigDecimal(body.get("rankAbilityFirst").toString()));
        if (body.containsKey("rankBasicFirst") && body.get("rankBasicFirst") != null)
            p.setRankBasicFirst(new BigDecimal(body.get("rankBasicFirst").toString()));
        return p;
    }

    @SuppressWarnings("unchecked")
    private void saveLevelsAndCriteria(Long projectId, Map<String, Object> body) {
        List<Map<String, Object>> levels = (List<Map<String, Object>>) body.get("levels");
        if (levels != null) {
            for (Map<String, Object> lv : levels) {
                ScholarshipLevel l = new ScholarshipLevel();
                l.setProjectId(projectId);
                l.setLevelName((String) lv.get("levelName"));
                l.setLevelOrder(((Number) lv.get("levelOrder")).intValue());
                l.setRatio(lv.get("ratio") == null ? null : new BigDecimal(lv.get("ratio").toString()));
                l.setAmount(lv.get("amount") == null ? null : new BigDecimal(lv.get("amount").toString()));
                if (lv.get("rankBasicMaxRatio") != null)
                    l.setRankBasicMaxRatio(new BigDecimal(lv.get("rankBasicMaxRatio").toString()));
                if (lv.get("rankAbilityMaxRatio") != null)
                    l.setRankAbilityMaxRatio(new BigDecimal(lv.get("rankAbilityMaxRatio").toString()));
                levelMapper.insert(l);
            }
        }
        List<Map<String, Object>> criteria = (List<Map<String, Object>>) body.get("criteria");
        if (criteria != null) {
            for (Map<String, Object> c : criteria) {
                ScholarshipCriterion cr = new ScholarshipCriterion();
                cr.setProjectId(projectId);
                cr.setRuleType((String) c.get("ruleType"));
                cr.setRuleValue(c.get("ruleValue") == null ? null : c.get("ruleValue").toString());
                criterionMapper.insert(cr);
            }
        }
    }

    private String buildUnassignedReason(Application app, Student stu, int totalQuota) {
        if (app.getSnapshotAbilityRank() == null) return "尚未执行排名计算";
        if (totalQuota > 0 && app.getSnapshotAbilityRank() > totalQuota)
            return String.format("综合能力排名第 %d 名，超出名额限制（共 %d 个名额）", app.getSnapshotAbilityRank(), totalQuota);
        return "排名超出所有等级名额";
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }
}
