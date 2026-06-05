package com.zjsu.scholarship.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zjsu.scholarship.common.BusinessException;
import com.zjsu.scholarship.common.R;
import com.zjsu.scholarship.entity.*;
import com.zjsu.scholarship.mapper.*;
import com.zjsu.scholarship.security.AuthContext;
import com.zjsu.scholarship.security.RequireRole;
import com.zjsu.scholarship.service.EvaluationService;
import com.zjsu.scholarship.service.FileStorageService;
import com.zjsu.scholarship.service.RankingService;
import com.zjsu.scholarship.service.ScoreCalcService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@RequireRole("STUDENT")
public class StudentController {

    private final StudentMapper studentMapper;
    private final AcademicYearMapper yearMapper;
    private final EvaluationRecordMapper evalMapper;
    private final CourseGradeMapper courseGradeMapper;
    private final MoralAppraisalMapper moralAppraisalMapper;
    private final MoralRecordItemMapper moralRecordMapper;
    private final ResearchInnovationItemMapper riMapper;
    private final ProfessionalSkillItemMapper psMapper;
    private final OrganizationWorkItemMapper owMapper;
    private final SportsAestheticsItemMapper saMapper;
    private final LaborPracticeItemMapper lpMapper;
    private final ScholarshipProjectMapper projectMapper;
    private final ScholarshipLevelMapper levelMapper;
    private final ApplicationMapper applicationMapper;
    private final EvaluationService evaluationService;
    private final FileStorageService fileStorageService;
    private final ScoreCalcService scoreCalcService;
    private final RankingService rankingService;

    public StudentController(StudentMapper studentMapper, AcademicYearMapper yearMapper,
                             EvaluationRecordMapper evalMapper, CourseGradeMapper courseGradeMapper,
                             MoralAppraisalMapper moralAppraisalMapper,
                             MoralRecordItemMapper moralRecordMapper,
                             ResearchInnovationItemMapper riMapper,
                             ProfessionalSkillItemMapper psMapper,
                             OrganizationWorkItemMapper owMapper,
                             SportsAestheticsItemMapper saMapper,
                             LaborPracticeItemMapper lpMapper,
                             ScholarshipProjectMapper projectMapper,
                             ScholarshipLevelMapper levelMapper,
                             ApplicationMapper applicationMapper,
                             EvaluationService evaluationService,
                             FileStorageService fileStorageService,
                             ScoreCalcService scoreCalcService,
                             RankingService rankingService) {
        this.studentMapper = studentMapper;
        this.yearMapper = yearMapper;
        this.evalMapper = evalMapper;
        this.courseGradeMapper = courseGradeMapper;
        this.moralAppraisalMapper = moralAppraisalMapper;
        this.moralRecordMapper = moralRecordMapper;
        this.riMapper = riMapper;
        this.psMapper = psMapper;
        this.owMapper = owMapper;
        this.saMapper = saMapper;
        this.lpMapper = lpMapper;
        this.projectMapper = projectMapper;
        this.levelMapper = levelMapper;
        this.applicationMapper = applicationMapper;
        this.evaluationService = evaluationService;
        this.fileStorageService = fileStorageService;
        this.scoreCalcService = scoreCalcService;
        this.rankingService = rankingService;
    }

    // ===== 工具方法 =====
    private Student curStudent() {
        String account = AuthContext.get().account;
        Student s = studentMapper.selectOne(Wrappers.<Student>lambdaQuery()
                .eq(Student::getStudentNo, account));
        if (s == null) throw new BusinessException("学生档案未找到");
        return s;
    }

    private AcademicYear curYear() {
        return yearMapper.selectOne(Wrappers.<AcademicYear>lambdaQuery()
                .eq(AcademicYear::getStatus, "ACTIVE")
                .orderByDesc(AcademicYear::getId)
                .last("LIMIT 1"));
    }

    private void assertOwn(Long evalId) {
        Student s = curStudent();
        EvaluationRecord rec = evalMapper.selectById(evalId);
        if (rec == null || !Objects.equals(rec.getStudentId(), s.getId())) {
            throw new BusinessException("无权操作他人数据");
        }
    }

    // ===== 基本信息 =====
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        Student s = curStudent();
        AcademicYear y = curYear();
        EvaluationRecord rec = y == null ? null : evaluationService.findOrCreate(s.getId(), y.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("student", s);
        data.put("year", y);
        data.put("evaluation", rec);
        return R.ok(data);
    }

    // ===== 综测填报页面数据 =====
    @GetMapping("/evaluation/items")
    public R<Map<String, Object>> items() {
        Student s = curStudent();
        AcademicYear y = curYear();
        if (y == null) throw new BusinessException("当前没有有效学年");
        EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());
        Long evalId = rec.getId();

        Map<String, Object> data = new HashMap<>();
        data.put("evaluation", rec);
        // 基本项
        data.put("appraisals", moralAppraisalMapper.selectList(
                Wrappers.<MoralAppraisal>lambdaQuery().eq(MoralAppraisal::getEvaluationId, evalId)));
        data.put("moralRecords", moralRecordMapper.selectList(
                Wrappers.<MoralRecordItem>lambdaQuery().eq(MoralRecordItem::getEvaluationId, evalId)
                        .orderByDesc(MoralRecordItem::getCreatedAt)));
        data.put("courses", courseGradeMapper.selectList(
                Wrappers.<CourseGrade>lambdaQuery().eq(CourseGrade::getStudentId, s.getId())
                        .eq(CourseGrade::getAcademicYearId, y.getId())));
        // 综合能力五模块
        data.put("riItems", riMapper.selectList(
                Wrappers.<ResearchInnovationItem>lambdaQuery().eq(ResearchInnovationItem::getEvaluationId, evalId)
                        .orderByDesc(ResearchInnovationItem::getCreatedAt)));
        data.put("psItems", psMapper.selectList(
                Wrappers.<ProfessionalSkillItem>lambdaQuery().eq(ProfessionalSkillItem::getEvaluationId, evalId)
                        .orderByDesc(ProfessionalSkillItem::getCreatedAt)));
        data.put("owItems", owMapper.selectList(
                Wrappers.<OrganizationWorkItem>lambdaQuery().eq(OrganizationWorkItem::getEvaluationId, evalId)
                        .orderByDesc(OrganizationWorkItem::getCreatedAt)));
        data.put("saItems", saMapper.selectList(
                Wrappers.<SportsAestheticsItem>lambdaQuery().eq(SportsAestheticsItem::getEvaluationId, evalId)
                        .orderByDesc(SportsAestheticsItem::getCreatedAt)));
        data.put("lpItems", lpMapper.selectList(
                Wrappers.<LaborPracticeItem>lambdaQuery().eq(LaborPracticeItem::getEvaluationId, evalId)
                        .orderByDesc(LaborPracticeItem::getCreatedAt)));
        return R.ok(data);
    }

    // ===== 提交 =====
    @PostMapping("/evaluation/submit")
    public R<Map<String, Object>> submitEval() {
        Student s = curStudent();
        AcademicYear y = curYear();
        evaluationService.submit(s.getId(), y.getId());
        EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("evaluation", rec);
        return R.ok(data);
    }

    // ===== 上传 =====
    @PostMapping("/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return R.ok(data);
    }

    // ===== 品德评议 CRUD =====
    @PostMapping("/evaluation/appraisals")
    public R<MoralAppraisal> addAppraisal(@RequestBody MoralAppraisal item) {
        Student s = curStudent();
        AcademicYear y = curYear();
        EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());
        item.setId(null);
        item.setEvaluationId(rec.getId());
        moralAppraisalMapper.insert(item);
        evaluationService.recalculateBasic(s.getId(), y.getId());
        return R.ok(item);
    }

    @PutMapping("/evaluation/appraisals/{id}")
    public R<MoralAppraisal> updateAppraisal(@PathVariable Long id, @RequestBody MoralAppraisal update) {
        MoralAppraisal existing = moralAppraisalMapper.selectById(id);
        if (existing == null) throw new BusinessException("记录不存在");
        assertOwn(existing.getEvaluationId());
        update.setId(id);
        update.setEvaluationId(existing.getEvaluationId());
        moralAppraisalMapper.updateById(update);
        Student s = curStudent();
        AcademicYear y = curYear();
        evaluationService.recalculateBasic(s.getId(), y.getId());
        return R.ok(update);
    }

    // ===== 品德记实 CRUD =====
    @PostMapping("/evaluation/moral-records")
    public R<MoralRecordItem> addMoralRecord(@RequestBody MoralRecordItem item) {
        Student s = curStudent();
        AcademicYear y = curYear();
        EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());
        item.setId(null);
        item.setEvaluationId(rec.getId());
        item.setReviewStatus("PENDING");
        item.setCreatedAt(LocalDateTime.now());
        item.setScore(scoreCalcService.moralRecordScore(Collections.singletonList(item)));
        moralRecordMapper.insert(item);
        evaluationService.recalculateBasic(s.getId(), y.getId());
        return R.ok(item);
    }

    @PutMapping("/evaluation/moral-records/{id}")
    public R<MoralRecordItem> updateMoralRecord(@PathVariable Long id, @RequestBody MoralRecordItem update) {
        MoralRecordItem existing = moralRecordMapper.selectById(id);
        if (existing == null) throw new BusinessException("记录不存在");
        assertOwn(existing.getEvaluationId());
        if ("APPROVED".equals(existing.getReviewStatus()))
            throw new BusinessException("已审核通过的记录不可修改");
        update.setId(id);
        update.setEvaluationId(existing.getEvaluationId());
        update.setReviewStatus("PENDING");
        update.setReviewRemark(null);
        update.setCreatedAt(existing.getCreatedAt());
        update.setScore(scoreCalcService.moralRecordScore(Collections.singletonList(update)));
        moralRecordMapper.updateById(update);
        Student s = curStudent();
        AcademicYear y = curYear();
        evaluationService.recalculateBasic(s.getId(), y.getId());
        return R.ok(update);
    }

    @DeleteMapping("/evaluation/moral-records/{id}")
    public R<Void> delMoralRecord(@PathVariable Long id) {
        MoralRecordItem m = moralRecordMapper.selectById(id);
        if (m == null) return R.ok();
        assertOwn(m.getEvaluationId());
        if ("APPROVED".equals(m.getReviewStatus()))
            throw new BusinessException("已审核通过的记录不可删除");
        moralRecordMapper.deleteById(id);
        Student s = curStudent();
        EvaluationRecord rec = evalMapper.selectById(m.getEvaluationId());
        if (rec != null) evaluationService.recalculateBasic(s.getId(), rec.getAcademicYearId());
        return R.ok();
    }

    // ===== 研究创新 CRUD =====
    @PostMapping("/evaluation/ri-items")
    public R<ResearchInnovationItem> addRI(@RequestBody ResearchInnovationItem item) {
        return addRIitem(item);
    }

    @PutMapping("/evaluation/ri-items/{id}")
    public R<ResearchInnovationItem> updateRI(@PathVariable Long id, @RequestBody ResearchInnovationItem update) {
        return updateRIitem(id, update);
    }

    @DeleteMapping("/evaluation/ri-items/{id}")
    public R<Void> delRI(@PathVariable Long id) {
        return deleteAbilityItem(id, "ri");
    }

    // ===== 专业技能 CRUD =====
    @PostMapping("/evaluation/ps-items")
    public R<ProfessionalSkillItem> addPS(@RequestBody ProfessionalSkillItem item) {
        Student s = curStudent();
        AcademicYear y = curYear();
        EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());
        item.setId(null);
        item.setEvaluationId(rec.getId());
        item.setReviewStatus("PENDING");
        item.setCreatedAt(LocalDateTime.now());
        item.setScore(scoreCalcService.professionalSkill(item));
        psMapper.insert(item);
        evaluationService.recalculateAbility(s.getId(), y.getId());
        return R.ok(item);
    }

    @PutMapping("/evaluation/ps-items/{id}")
    public R<ProfessionalSkillItem> updatePS(@PathVariable Long id, @RequestBody ProfessionalSkillItem update) {
        ProfessionalSkillItem existing = psMapper.selectById(id);
        if (existing == null) throw new BusinessException("记录不存在");
        assertOwn(existing.getEvaluationId());
        if ("APPROVED".equals(existing.getReviewStatus()))
            throw new BusinessException("已审核通过的记录不可修改");
        update.setId(id);
        update.setEvaluationId(existing.getEvaluationId());
        update.setReviewStatus("PENDING");
        update.setReviewRemark(null);
        update.setCreatedAt(existing.getCreatedAt());
        update.setScore(scoreCalcService.professionalSkill(update));
        psMapper.updateById(update);
        Student s = curStudent();
        AcademicYear y = curYear();
        evaluationService.recalculateAbility(s.getId(), y.getId());
        return R.ok(update);
    }

    @DeleteMapping("/evaluation/ps-items/{id}")
    public R<Void> delPS(@PathVariable Long id) {
        return deleteAbilityItem(id, "ps");
    }

    // ===== 组织工作 CRUD =====
    @PostMapping("/evaluation/ow-items")
    public R<OrganizationWorkItem> addOW(@RequestBody OrganizationWorkItem item) {
        Student s = curStudent();
        AcademicYear y = curYear();
        EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());
        item.setId(null);
        item.setEvaluationId(rec.getId());
        item.setReviewStatus("PENDING");
        item.setCreatedAt(LocalDateTime.now());
        item.setScore(scoreCalcService.organizationWork(item));
        owMapper.insert(item);
        evaluationService.recalculateAbility(s.getId(), y.getId());
        return R.ok(item);
    }

    @PutMapping("/evaluation/ow-items/{id}")
    public R<OrganizationWorkItem> updateOW(@PathVariable Long id, @RequestBody OrganizationWorkItem update) {
        OrganizationWorkItem existing = owMapper.selectById(id);
        if (existing == null) throw new BusinessException("记录不存在");
        assertOwn(existing.getEvaluationId());
        if ("APPROVED".equals(existing.getReviewStatus()))
            throw new BusinessException("已审核通过的记录不可修改");
        update.setId(id);
        update.setEvaluationId(existing.getEvaluationId());
        update.setReviewStatus("PENDING");
        update.setReviewRemark(null);
        update.setCreatedAt(existing.getCreatedAt());
        update.setScore(scoreCalcService.organizationWork(update));
        owMapper.updateById(update);
        Student s = curStudent();
        AcademicYear y = curYear();
        evaluationService.recalculateAbility(s.getId(), y.getId());
        return R.ok(update);
    }

    @DeleteMapping("/evaluation/ow-items/{id}")
    public R<Void> delOW(@PathVariable Long id) {
        return deleteAbilityItem(id, "ow");
    }

    // ===== 体育美育 CRUD =====
    @PostMapping("/evaluation/sa-items")
    public R<SportsAestheticsItem> addSA(@RequestBody SportsAestheticsItem item) {
        Student s = curStudent();
        AcademicYear y = curYear();
        EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());
        item.setId(null);
        item.setEvaluationId(rec.getId());
        item.setReviewStatus("PENDING");
        item.setCreatedAt(LocalDateTime.now());
        item.setScore(scoreCalcService.sportsAesthetics(item));
        saMapper.insert(item);
        evaluationService.recalculateAbility(s.getId(), y.getId());
        return R.ok(item);
    }

    @PutMapping("/evaluation/sa-items/{id}")
    public R<SportsAestheticsItem> updateSA(@PathVariable Long id, @RequestBody SportsAestheticsItem update) {
        SportsAestheticsItem existing = saMapper.selectById(id);
        if (existing == null) throw new BusinessException("记录不存在");
        assertOwn(existing.getEvaluationId());
        if ("APPROVED".equals(existing.getReviewStatus()))
            throw new BusinessException("已审核通过的记录不可修改");
        update.setId(id);
        update.setEvaluationId(existing.getEvaluationId());
        update.setReviewStatus("PENDING");
        update.setReviewRemark(null);
        update.setCreatedAt(existing.getCreatedAt());
        update.setScore(scoreCalcService.sportsAesthetics(update));
        saMapper.updateById(update);
        Student s = curStudent();
        AcademicYear y = curYear();
        evaluationService.recalculateAbility(s.getId(), y.getId());
        return R.ok(update);
    }

    @DeleteMapping("/evaluation/sa-items/{id}")
    public R<Void> delSA(@PathVariable Long id) {
        return deleteAbilityItem(id, "sa");
    }

    // ===== 劳动教育 CRUD =====
    @PostMapping("/evaluation/lp-items")
    public R<LaborPracticeItem> addLP(@RequestBody LaborPracticeItem item) {
        Student s = curStudent();
        AcademicYear y = curYear();
        EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());
        item.setId(null);
        item.setEvaluationId(rec.getId());
        item.setReviewStatus("PENDING");
        item.setCreatedAt(LocalDateTime.now());
        item.setScore(scoreCalcService.laborPractice(item));
        lpMapper.insert(item);
        evaluationService.recalculateAbility(s.getId(), y.getId());
        return R.ok(item);
    }

    @PutMapping("/evaluation/lp-items/{id}")
    public R<LaborPracticeItem> updateLP(@PathVariable Long id, @RequestBody LaborPracticeItem update) {
        LaborPracticeItem existing = lpMapper.selectById(id);
        if (existing == null) throw new BusinessException("记录不存在");
        assertOwn(existing.getEvaluationId());
        if ("APPROVED".equals(existing.getReviewStatus()))
            throw new BusinessException("已审核通过的记录不可修改");
        update.setId(id);
        update.setEvaluationId(existing.getEvaluationId());
        update.setReviewStatus("PENDING");
        update.setReviewRemark(null);
        update.setCreatedAt(existing.getCreatedAt());
        update.setScore(scoreCalcService.laborPractice(update));
        lpMapper.updateById(update);
        Student s = curStudent();
        AcademicYear y = curYear();
        evaluationService.recalculateAbility(s.getId(), y.getId());
        return R.ok(update);
    }

    @DeleteMapping("/evaluation/lp-items/{id}")
    public R<Void> delLP(@PathVariable Long id) {
        return deleteAbilityItem(id, "lp");
    }

    // ===== 通用删除能力项 =====
    private R<Void> deleteAbilityItem(Long id, String kind) {
        Object item = null;
        Long evalId = null;
        switch (kind) {
            case "ri" -> { ResearchInnovationItem r = riMapper.selectById(id); if (r != null) { evalId = r.getEvaluationId(); item = r; } }
            case "ps" -> { ProfessionalSkillItem r = psMapper.selectById(id); if (r != null) { evalId = r.getEvaluationId(); item = r; } }
            case "ow" -> { OrganizationWorkItem r = owMapper.selectById(id); if (r != null) { evalId = r.getEvaluationId(); item = r; } }
            case "sa" -> { SportsAestheticsItem r = saMapper.selectById(id); if (r != null) { evalId = r.getEvaluationId(); item = r; } }
            case "lp" -> { LaborPracticeItem r = lpMapper.selectById(id); if (r != null) { evalId = r.getEvaluationId(); item = r; } }
        }
        if (item == null) return R.ok();
        assertOwn(evalId);
        // Check not approved
        boolean approved = false;
        switch (kind) {
            case "ri" -> approved = "APPROVED".equals(((ResearchInnovationItem) item).getReviewStatus());
            case "ps" -> approved = "APPROVED".equals(((ProfessionalSkillItem) item).getReviewStatus());
            case "ow" -> approved = "APPROVED".equals(((OrganizationWorkItem) item).getReviewStatus());
            case "sa" -> approved = "APPROVED".equals(((SportsAestheticsItem) item).getReviewStatus());
            case "lp" -> approved = "APPROVED".equals(((LaborPracticeItem) item).getReviewStatus());
        }
        if (approved) throw new BusinessException("已审核通过的记录不可删除");

        switch (kind) {
            case "ri" -> riMapper.deleteById(id);
            case "ps" -> psMapper.deleteById(id);
            case "ow" -> owMapper.deleteById(id);
            case "sa" -> saMapper.deleteById(id);
            case "lp" -> lpMapper.deleteById(id);
        }
        Student s = curStudent();
        EvaluationRecord rec = evalMapper.selectById(evalId);
        if (rec != null) evaluationService.recalculateAbility(s.getId(), rec.getAcademicYearId());
        return R.ok();
    }

    // ===== 研究创新 内部辅助 =====
    private R<ResearchInnovationItem> addRIitem(ResearchInnovationItem item) {
        Student s = curStudent();
        AcademicYear y = curYear();
        EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());
        item.setId(null);
        item.setEvaluationId(rec.getId());
        item.setReviewStatus("PENDING");
        item.setCreatedAt(LocalDateTime.now());
        item.setScore(scoreCalcService.researchInnovation(item));
        riMapper.insert(item);
        evaluationService.recalculateAbility(s.getId(), y.getId());
        return R.ok(item);
    }

    private R<ResearchInnovationItem> updateRIitem(Long id, ResearchInnovationItem update) {
        ResearchInnovationItem existing = riMapper.selectById(id);
        if (existing == null) throw new BusinessException("记录不存在");
        assertOwn(existing.getEvaluationId());
        if ("APPROVED".equals(existing.getReviewStatus()))
            throw new BusinessException("已审核通过的记录不可修改");
        update.setId(id);
        update.setEvaluationId(existing.getEvaluationId());
        update.setReviewStatus("PENDING");
        update.setReviewRemark(null);
        update.setCreatedAt(existing.getCreatedAt());
        update.setScore(scoreCalcService.researchInnovation(update));
        riMapper.updateById(update);
        Student s = curStudent();
        AcademicYear y = curYear();
        evaluationService.recalculateAbility(s.getId(), y.getId());
        return R.ok(update);
    }

    // ===== 奖学金申报 =====
    @GetMapping("/scholarships/eligible")
    public R<List<Map<String, Object>>> eligible() {
        Student s = curStudent();
        AcademicYear y = curYear();
        if (y == null) return R.ok(Collections.emptyList());
        EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());

        List<ScholarshipProject> projects = projectMapper.selectList(
                Wrappers.<ScholarshipProject>lambdaQuery()
                        .eq(ScholarshipProject::getAcademicYearId, y.getId())
                        .in(ScholarshipProject::getStatus, Arrays.asList("OPEN", "REVIEWING", "PUBLISHED")));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ScholarshipProject p : projects) {
            Map<String, Object> row = new HashMap<>();
            row.put("project", p);
            row.put("levels", levelMapper.selectList(
                    Wrappers.<ScholarshipLevel>lambdaQuery()
                            .eq(ScholarshipLevel::getProjectId, p.getId())
                            .orderByAsc(ScholarshipLevel::getLevelOrder)));
            row.put("recommendLevelId", rankingService.previewLevelForStudent(s.getId(), p.getId()));
            row.put("eligibilityCheck", rankingService.checkEligibility(s, rec, p));
            Application existing = applicationMapper.selectOne(
                    Wrappers.<Application>lambdaQuery()
                            .eq(Application::getStudentId, s.getId())
                            .eq(Application::getProjectId, p.getId()));
            row.put("application", existing);
            row.put("evaluation", rec);
            result.add(row);
        }
        return R.ok(result);
    }

    @PostMapping("/applications")
    public R<Application> apply(@RequestBody Map<String, Object> body) {
        Long projectId = ((Number) body.get("projectId")).longValue();
        Student s = curStudent();
        AcademicYear y = curYear();
        ScholarshipProject p = projectMapper.selectById(projectId);
        if (p == null) throw new BusinessException("奖学金项目不存在");
        if (!"OPEN".equals(p.getStatus()) && !"REVIEWING".equals(p.getStatus()))
            throw new BusinessException("当前项目不在申报期");

        Application exist = applicationMapper.selectOne(
                Wrappers.<Application>lambdaQuery()
                        .eq(Application::getStudentId, s.getId())
                        .eq(Application::getProjectId, projectId));
        if (exist != null) throw new BusinessException("您已申请过该项目");

        EvaluationRecord rec = evaluationService.findOrCreate(s.getId(), y.getId());

        // 硬性条件校验
        String eligibility = rankingService.checkEligibility(s, rec, p);
        if (eligibility != null) throw new BusinessException("不符合申报条件：" + eligibility);

        Application app = new Application();
        app.setStudentId(s.getId());
        app.setProjectId(projectId);
        app.setEvaluationId(rec.getId());
        app.setSnapshotBasicTotal(rec.getBasicTotal());
        app.setSnapshotBasicRank(rec.getBasicRank());
        app.setSnapshotAbilityTotal(rec.getAbilityTotal());
        app.setSnapshotAbilityRank(rec.getAbilityRank());
        app.setAutoLevelId(rankingService.previewLevelForStudent(s.getId(), projectId));
        app.setStatus("SUBMITTED");
        app.setSubmittedAt(LocalDateTime.now());
        applicationMapper.insert(app);
        return R.ok(app);
    }

    @GetMapping("/applications")
    public R<List<Map<String, Object>>> myApplications() {
        Student s = curStudent();
        List<Application> apps = applicationMapper.selectList(
                Wrappers.<Application>lambdaQuery()
                        .eq(Application::getStudentId, s.getId())
                        .orderByDesc(Application::getSubmittedAt));
        return R.ok(apps.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            ScholarshipProject p = projectMapper.selectById(a.getProjectId());
            m.put("application", a);
            m.put("project", p);
            if (a.getAutoLevelId() != null) m.put("autoLevel", levelMapper.selectById(a.getAutoLevelId()));
            if (a.getFinalLevelId() != null) m.put("finalLevel", levelMapper.selectById(a.getFinalLevelId()));
            return m;
        }).collect(Collectors.toList()));
    }

    @DeleteMapping("/applications/{id}")
    public R<Void> withdraw(@PathVariable Long id) {
        Application app = applicationMapper.selectById(id);
        if (app == null) throw new BusinessException("申请不存在");
        Student s = curStudent();
        if (!Objects.equals(app.getStudentId(), s.getId()))
            throw new BusinessException("无权操作");
        if (!"SUBMITTED".equals(app.getStatus()))
            throw new BusinessException("已审核或已发布的申请不可撤回");
        applicationMapper.deleteById(id);
        return R.ok();
    }
}
