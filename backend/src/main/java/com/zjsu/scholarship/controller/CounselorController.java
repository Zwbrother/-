package com.zjsu.scholarship.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zjsu.scholarship.common.BusinessException;
import com.zjsu.scholarship.common.R;
import com.zjsu.scholarship.entity.*;
import com.zjsu.scholarship.mapper.*;
import com.zjsu.scholarship.security.AuthContext;
import com.zjsu.scholarship.security.RequireRole;
import com.zjsu.scholarship.service.EvaluationService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/counselor")
@RequireRole({"COUNSELOR", "ADMIN"})
public class CounselorController {

    private final StudentMapper studentMapper;
    private final AcademicYearMapper yearMapper;
    private final EvaluationRecordMapper evalMapper;
    private final MoralAppraisalMapper moralAppraisalMapper;
    private final MoralRecordItemMapper moralRecordMapper;
    private final ResearchInnovationItemMapper riMapper;
    private final ProfessionalSkillItemMapper psMapper;
    private final OrganizationWorkItemMapper owMapper;
    private final SportsAestheticsItemMapper saMapper;
    private final LaborPracticeItemMapper lpMapper;
    private final ApplicationMapper applicationMapper;
    private final ScholarshipProjectMapper projectMapper;
    private final ScholarshipLevelMapper levelMapper;
    private final EvaluationService evaluationService;
    private final GraduateExamApplicationMapper geMapper;

    public CounselorController(StudentMapper studentMapper, AcademicYearMapper yearMapper,
                               EvaluationRecordMapper evalMapper,
                               MoralAppraisalMapper moralAppraisalMapper,
                               MoralRecordItemMapper moralRecordMapper,
                               ResearchInnovationItemMapper riMapper,
                               ProfessionalSkillItemMapper psMapper,
                               OrganizationWorkItemMapper owMapper,
                               SportsAestheticsItemMapper saMapper,
                               LaborPracticeItemMapper lpMapper,
                               ApplicationMapper applicationMapper,
                               ScholarshipProjectMapper projectMapper,
                               ScholarshipLevelMapper levelMapper,
                               EvaluationService evaluationService,
                               GraduateExamApplicationMapper geMapper) {
        this.studentMapper = studentMapper;
        this.yearMapper = yearMapper;
        this.evalMapper = evalMapper;
        this.moralAppraisalMapper = moralAppraisalMapper;
        this.moralRecordMapper = moralRecordMapper;
        this.riMapper = riMapper;
        this.psMapper = psMapper;
        this.owMapper = owMapper;
        this.saMapper = saMapper;
        this.lpMapper = lpMapper;
        this.applicationMapper = applicationMapper;
        this.projectMapper = projectMapper;
        this.levelMapper = levelMapper;
        this.evaluationService = evaluationService;
        this.geMapper = geMapper;
    }

    private List<Student> allStudents(String major) {
        return studentMapper.selectList(Wrappers.<Student>lambdaQuery()
                .eq(major != null && !major.isEmpty(), Student::getMajor, major));
    }

    @GetMapping("/students")
    public R<List<Map<String, Object>>> students(@RequestParam(required = false) String major) {
        AcademicYear y = curYear();
        List<Student> stus = allStudents(major);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student s : stus) {
            Map<String, Object> row = new HashMap<>();
            row.put("student", s);
            if (y != null) {
                EvaluationRecord rec = evalMapper.selectOne(Wrappers.<EvaluationRecord>lambdaQuery()
                        .eq(EvaluationRecord::getStudentId, s.getId())
                        .eq(EvaluationRecord::getAcademicYearId, y.getId()));
                row.put("evaluation", rec);
            }
            result.add(row);
        }
        return R.ok(result);
    }

    @GetMapping("/items/pending")
    public R<Map<String, Object>> pendingItems() {
        AcademicYear y = curYear();
        List<Student> stus = allStudents(null);
        Set<Long> stuIds = stus.stream().map(Student::getId).collect(Collectors.toSet());
        Map<String, Object> data = new HashMap<>();
        if (stuIds.isEmpty()) return R.ok(fillEmpty(data));

        List<EvaluationRecord> recs = evalMapper.selectList(Wrappers.<EvaluationRecord>lambdaQuery()
                .in(EvaluationRecord::getStudentId, stuIds)
                .eq(y != null, EvaluationRecord::getAcademicYearId, y == null ? null : y.getId()));
        Set<Long> evalIds = recs.stream().map(EvaluationRecord::getId).collect(Collectors.toSet());
        Map<Long, Student> stuByEvalId = new HashMap<>();
        for (EvaluationRecord r : recs) {
            stus.stream().filter(s -> s.getId().equals(r.getStudentId()))
                    .findFirst().ifPresent(s -> stuByEvalId.put(r.getId(), s));
        }
        if (evalIds.isEmpty()) return R.ok(fillEmpty(data));

        data.put("moralRecords", wrap(moralRecordMapper.selectList(
                Wrappers.<MoralRecordItem>lambdaQuery()
                        .in(MoralRecordItem::getEvaluationId, evalIds)
                        .eq(MoralRecordItem::getReviewStatus, "PENDING")), stuByEvalId, i -> i.getEvaluationId()));
        data.put("riItems", wrap(riMapper.selectList(
                Wrappers.<ResearchInnovationItem>lambdaQuery()
                        .in(ResearchInnovationItem::getEvaluationId, evalIds)
                        .eq(ResearchInnovationItem::getReviewStatus, "PENDING")), stuByEvalId, i -> i.getEvaluationId()));
        data.put("psItems", wrap(psMapper.selectList(
                Wrappers.<ProfessionalSkillItem>lambdaQuery()
                        .in(ProfessionalSkillItem::getEvaluationId, evalIds)
                        .eq(ProfessionalSkillItem::getReviewStatus, "PENDING")), stuByEvalId, i -> i.getEvaluationId()));
        data.put("owItems", wrap(owMapper.selectList(
                Wrappers.<OrganizationWorkItem>lambdaQuery()
                        .in(OrganizationWorkItem::getEvaluationId, evalIds)
                        .eq(OrganizationWorkItem::getReviewStatus, "PENDING")), stuByEvalId, i -> i.getEvaluationId()));
        data.put("saItems", wrap(saMapper.selectList(
                Wrappers.<SportsAestheticsItem>lambdaQuery()
                        .in(SportsAestheticsItem::getEvaluationId, evalIds)
                        .eq(SportsAestheticsItem::getReviewStatus, "PENDING")), stuByEvalId, i -> i.getEvaluationId()));
        data.put("lpItems", wrap(lpMapper.selectList(
                Wrappers.<LaborPracticeItem>lambdaQuery()
                        .in(LaborPracticeItem::getEvaluationId, evalIds)
                        .eq(LaborPracticeItem::getReviewStatus, "PENDING")), stuByEvalId, i -> i.getEvaluationId()));
        return R.ok(data);
    }

    private Map<String, Object> fillEmpty(Map<String, Object> data) {
        data.put("moralRecords", Collections.emptyList());
        data.put("riItems", Collections.emptyList());
        data.put("psItems", Collections.emptyList());
        data.put("owItems", Collections.emptyList());
        data.put("saItems", Collections.emptyList());
        data.put("lpItems", Collections.emptyList());
        return data;
    }

    private <T> List<Map<String, Object>> wrap(List<T> items, Map<Long, Student> stuByEvalId,
                                               java.util.function.Function<T, Long> evalIdFn) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (T item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("item", item);
            m.put("student", stuByEvalId.get(evalIdFn.apply(item)));
            list.add(m);
        }
        return list;
    }

    // ===== 审核端点 =====
    @PostMapping("/items/moral-record/{id}/review")
    public R<Void> reviewMoralRecord(@PathVariable Long id, @RequestBody Map<String, String> body) {
        MoralRecordItem item = moralRecordMapper.selectById(id);
        if (item == null) throw new BusinessException("记录不存在");
        item.setReviewStatus(body.get("status"));
        item.setReviewRemark(body.get("remark"));
        moralRecordMapper.updateById(item);
        EvaluationRecord rec = evalMapper.selectById(item.getEvaluationId());
        if (rec != null) evaluationService.recalculateBasic(rec.getStudentId(), rec.getAcademicYearId());
        return R.ok();
    }

    @PostMapping("/items/ri/{id}/review")
    public R<Void> reviewRI(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ResearchInnovationItem item = riMapper.selectById(id);
        if (item == null) throw new BusinessException("记录不存在");
        item.setReviewStatus(body.get("status"));
        item.setReviewRemark(body.get("remark"));
        riMapper.updateById(item);
        EvaluationRecord rec = evalMapper.selectById(item.getEvaluationId());
        if (rec != null) evaluationService.recalculateAbility(rec.getStudentId(), rec.getAcademicYearId());
        return R.ok();
    }

    @PostMapping("/items/ps/{id}/review")
    public R<Void> reviewPS(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ProfessionalSkillItem item = psMapper.selectById(id);
        if (item == null) throw new BusinessException("记录不存在");
        item.setReviewStatus(body.get("status"));
        item.setReviewRemark(body.get("remark"));
        psMapper.updateById(item);
        EvaluationRecord rec = evalMapper.selectById(item.getEvaluationId());
        if (rec != null) evaluationService.recalculateAbility(rec.getStudentId(), rec.getAcademicYearId());
        return R.ok();
    }

    @PostMapping("/items/ow/{id}/review")
    public R<Void> reviewOW(@PathVariable Long id, @RequestBody Map<String, String> body) {
        OrganizationWorkItem item = owMapper.selectById(id);
        if (item == null) throw new BusinessException("记录不存在");
        item.setReviewStatus(body.get("status"));
        item.setReviewRemark(body.get("remark"));
        owMapper.updateById(item);
        EvaluationRecord rec = evalMapper.selectById(item.getEvaluationId());
        if (rec != null) evaluationService.recalculateAbility(rec.getStudentId(), rec.getAcademicYearId());
        return R.ok();
    }

    @PostMapping("/items/sa/{id}/review")
    public R<Void> reviewSA(@PathVariable Long id, @RequestBody Map<String, String> body) {
        SportsAestheticsItem item = saMapper.selectById(id);
        if (item == null) throw new BusinessException("记录不存在");
        item.setReviewStatus(body.get("status"));
        item.setReviewRemark(body.get("remark"));
        saMapper.updateById(item);
        EvaluationRecord rec = evalMapper.selectById(item.getEvaluationId());
        if (rec != null) evaluationService.recalculateAbility(rec.getStudentId(), rec.getAcademicYearId());
        return R.ok();
    }

    @PostMapping("/items/lp/{id}/review")
    public R<Void> reviewLP(@PathVariable Long id, @RequestBody Map<String, String> body) {
        LaborPracticeItem item = lpMapper.selectById(id);
        if (item == null) throw new BusinessException("记录不存在");
        item.setReviewStatus(body.get("status"));
        item.setReviewRemark(body.get("remark"));
        lpMapper.updateById(item);
        EvaluationRecord rec = evalMapper.selectById(item.getEvaluationId());
        if (rec != null) evaluationService.recalculateAbility(rec.getStudentId(), rec.getAcademicYearId());
        return R.ok();
    }

    // ===== 申请审核 =====
    @GetMapping("/applications")
    public R<List<Map<String, Object>>> applications(@RequestParam(required = false) String status) {
        List<Student> stus = allStudents(null);
        Set<Long> stuIds = stus.stream().map(Student::getId).collect(Collectors.toSet());
        if (stuIds.isEmpty()) return R.ok(Collections.emptyList());

        List<Application> apps = applicationMapper.selectList(Wrappers.<Application>lambdaQuery()
                .in(Application::getStudentId, stuIds)
                .eq(status != null && !status.isEmpty(), Application::getStatus, status)
                .orderByDesc(Application::getSubmittedAt));
        Map<Long, Student> stuMap = stus.stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Application app : apps) {
            Map<String, Object> m = new HashMap<>();
            m.put("application", app);
            m.put("student", stuMap.get(app.getStudentId()));
            m.put("project", projectMapper.selectById(app.getProjectId()));
            if (app.getAutoLevelId() != null) m.put("autoLevel", levelMapper.selectById(app.getAutoLevelId()));
            if (app.getFinalLevelId() != null) m.put("finalLevel", levelMapper.selectById(app.getFinalLevelId()));
            list.add(m);
        }
        return R.ok(list);
    }

    @PostMapping("/applications/{id}/review")
    public R<Void> reviewApp(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Application app = applicationMapper.selectById(id);
        if (app == null) throw new BusinessException("申请不存在");
        if (!"SUBMITTED".equals(app.getStatus()))
            throw new BusinessException("只能审核待审状态的申请");
        String status = (String) body.get("status");
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status))
            throw new BusinessException("status 只能为 APPROVED 或 REJECTED");
        app.setStatus(status);
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewerId(AuthContext.get().userId);
        if ("APPROVED".equals(status)) {
            Object lvl = body.get("finalLevelId");
            app.setFinalLevelId(lvl != null ? ((Number) lvl).longValue() : app.getAutoLevelId());
            app.setRejectReason(null);
        } else {
            app.setRejectReason((String) body.get("reason"));
            app.setFinalLevelId(null);
        }
        applicationMapper.updateById(app);
        return R.ok();
    }

    @PostMapping("/applications/batch-review")
    public R<Map<String, Object>> batchApprove(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("ids");
        int count = 0;
        if (ids != null) {
            for (Number idN : ids) {
                Application app = applicationMapper.selectById(idN.longValue());
                if (app == null || !"SUBMITTED".equals(app.getStatus())) continue;
                app.setStatus("APPROVED");
                app.setReviewedAt(LocalDateTime.now());
                app.setReviewerId(AuthContext.get().userId);
                app.setFinalLevelId(app.getAutoLevelId());
                applicationMapper.updateById(app);
                count++;
            }
        }
        return R.ok(Map.of("approved", count));
    }

    // ===== 考研奖学金审核 =====
    @GetMapping("/graduate-exam")
    public R<List<Map<String, Object>>> graduateExamApplications() {
        List<Student> stus = allStudents(null);
        Set<Long> stuIds = stus.stream().map(Student::getId).collect(Collectors.toSet());
        if (stuIds.isEmpty()) return R.ok(Collections.emptyList());

        List<GraduateExamApplication> apps = geMapper.selectList(
                Wrappers.<GraduateExamApplication>lambdaQuery()
                        .in(GraduateExamApplication::getStudentId, stuIds)
                        .orderByDesc(GraduateExamApplication::getSubmittedAt));
        Map<Long, Student> stuMap = stus.stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        List<Map<String, Object>> list = new ArrayList<>();
        for (GraduateExamApplication app : apps) {
            Map<String, Object> m = new HashMap<>();
            m.put("application", app);
            m.put("student", stuMap.get(app.getStudentId()));
            list.add(m);
        }
        return R.ok(list);
    }

    @PostMapping("/graduate-exam/{id}/review")
    public R<Void> reviewGraduateExam(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        GraduateExamApplication app = geMapper.selectById(id);
        if (app == null) throw new BusinessException("考研申报不存在");
        String status = (String) body.get("status");
        // 通过仅限待审状态；退回允许待审和已通过（辅导员可撤回已通过的审核）
        if ("APPROVED".equals(status) && !"SUBMITTED".equals(app.getStatus()))
            throw new BusinessException("只能在待审状态通过审核");
        if ("REJECTED".equals(status) && !"SUBMITTED".equals(app.getStatus()) && !"APPROVED".equals(app.getStatus()))
            throw new BusinessException("只能在待审或已通过状态退回");
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status))
            throw new BusinessException("status 只能为 APPROVED 或 REJECTED");
        app.setStatus(status);
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewerId(AuthContext.get().userId);
        if ("APPROVED".equals(status)) {
            app.setRejectReason(null);
            // 辅导员可覆盖等级，未指定则自动判定
            Object overrideLevel = body.get("finalLevel");
            if (overrideLevel != null && ("FIRST".equals(overrideLevel) || "SECOND".equals(overrideLevel))) {
                app.setFinalLevel((String) overrideLevel);
            } else if (Boolean.TRUE.equals(app.getIsAdmitted())) {
                app.setFinalLevel("FIRST");
            } else if (Boolean.TRUE.equals(app.getHasInterviewQualification())) {
                app.setFinalLevel("SECOND");
            }
        } else {
            app.setRejectReason((String) body.get("reason"));
        }
        geMapper.updateById(app);
        return R.ok();
    }

    // ===== 辅导员批量6维度评议 =====

    /**
     * 辅导员对学生进行批量6维度品德评议
     * Body: { "appraisals": [{"studentId": 1, "politicalLiteracy": 18, "legalAwareness": 18,
     *   "mentalQuality": 18, "integrityScore": 18, "teamwork": 18, "socialResponsibility": 18}, ...] }
     */
    @PostMapping("/batch-appraisal")
    @Transactional
    public R<Map<String, Object>> batchAppraisal(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> appraisals = (List<Map<String, Object>>) body.get("appraisals");
        AcademicYear y = curYear();
        if (y == null) throw new BusinessException("当前没有有效学年");

        int count = 0;
        for (Map<String, Object> a : appraisals) {
            Long studentId = ((Number) a.get("studentId")).longValue();
            EvaluationRecord rec = evaluationService.findOrCreate(studentId, y.getId());

            // 查找或创建辅导员评议记录
            MoralAppraisal existing = moralAppraisalMapper.selectOne(
                    Wrappers.<MoralAppraisal>lambdaQuery()
                            .eq(MoralAppraisal::getEvaluationId, rec.getId())
                            .eq(MoralAppraisal::getAppraiserType, "COUNSELOR"));

            MoralAppraisal appraisal = existing != null ? existing : new MoralAppraisal();
            appraisal.setEvaluationId(rec.getId());
            appraisal.setAppraiserType("COUNSELOR");
            appraisal.setPoliticalLiteracy(toDecimal(a.get("politicalLiteracy")));
            appraisal.setLegalAwareness(toDecimal(a.get("legalAwareness")));
            appraisal.setMentalQuality(toDecimal(a.get("mentalQuality")));
            appraisal.setIntegrityScore(toDecimal(a.get("integrityScore")));
            appraisal.setTeamwork(toDecimal(a.get("teamwork")));
            appraisal.setSocialResponsibility(toDecimal(a.get("socialResponsibility")));

            if (existing != null) {
                moralAppraisalMapper.updateById(appraisal);
            } else {
                moralAppraisalMapper.insert(appraisal);
            }

            evaluationService.recalculateBasic(studentId, y.getId());
            count++;
        }

        return R.ok(Map.of("count", count, "message", "辅导员批量评议完成"));
    }

    @GetMapping("/batch-appraisal/students")
    public R<List<Map<String, Object>>> batchAppraisalStudents(
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String className) {
        List<Student> students = studentMapper.selectList(Wrappers.<Student>lambdaQuery()
                .eq(major != null && !major.isEmpty(), Student::getMajor, major)
                .eq(className != null && !className.isEmpty(), Student::getClassName, className));
        AcademicYear y = curYear();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Student s : students) {
            Map<String, Object> row = new HashMap<>();
            row.put("student", s);
            if (y != null) {
                EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());
                row.put("evaluation", rec);
                MoralAppraisal existing = moralAppraisalMapper.selectOne(
                        Wrappers.<MoralAppraisal>lambdaQuery()
                                .eq(MoralAppraisal::getEvaluationId, rec.getId())
                                .eq(MoralAppraisal::getAppraiserType, "COUNSELOR"));
                row.put("appraisal", existing);
            }
            result.add(row);
        }
        return R.ok(result);
    }

    // ===== 工具 =====
    private java.math.BigDecimal toDecimal(Object o) {
        if (o == null) return java.math.BigDecimal.ZERO;
        if (o instanceof java.math.BigDecimal bd) return bd;
        return new java.math.BigDecimal(o.toString());
    }

    private AcademicYear curYear() {
        return yearMapper.selectOne(Wrappers.<AcademicYear>lambdaQuery()
                .eq(AcademicYear::getStatus, "ACTIVE")
                .orderByDesc(AcademicYear::getId)
                .last("LIMIT 1"));
    }
}
