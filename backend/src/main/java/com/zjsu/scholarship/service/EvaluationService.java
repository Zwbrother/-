package com.zjsu.scholarship.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zjsu.scholarship.entity.*;
import com.zjsu.scholarship.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 本科生综合评价服务（双轨制）
 * <p>
 * 基本项 = 品德总分 × 30% + 专业素质 × 70%
 * 综合能力 = 75 + 五模块加权
 * </p>
 */
@Service
public class EvaluationService {

    private final EvaluationRecordMapper evalMapper;
    private final StudentMapper studentMapper;
    private final CourseGradeMapper courseGradeMapper;
    private final MoralAppraisalMapper moralAppraisalMapper;
    private final MoralRecordItemMapper moralRecordMapper;
    private final ResearchInnovationItemMapper riMapper;
    private final ProfessionalSkillItemMapper psMapper;
    private final OrganizationWorkItemMapper owMapper;
    private final SportsAestheticsItemMapper saMapper;
    private final LaborPracticeItemMapper lpMapper;
    private final DisciplineRecordMapper disciplineMapper;
    private final ScoreCalcService calc;

    public EvaluationService(EvaluationRecordMapper evalMapper, StudentMapper studentMapper,
                             CourseGradeMapper courseGradeMapper,
                             MoralAppraisalMapper moralAppraisalMapper,
                             MoralRecordItemMapper moralRecordMapper,
                             ResearchInnovationItemMapper riMapper,
                             ProfessionalSkillItemMapper psMapper,
                             OrganizationWorkItemMapper owMapper,
                             SportsAestheticsItemMapper saMapper,
                             LaborPracticeItemMapper lpMapper,
                             DisciplineRecordMapper disciplineMapper,
                             ScoreCalcService calc) {
        this.evalMapper = evalMapper;
        this.studentMapper = studentMapper;
        this.courseGradeMapper = courseGradeMapper;
        this.moralAppraisalMapper = moralAppraisalMapper;
        this.moralRecordMapper = moralRecordMapper;
        this.riMapper = riMapper;
        this.psMapper = psMapper;
        this.owMapper = owMapper;
        this.saMapper = saMapper;
        this.lpMapper = lpMapper;
        this.disciplineMapper = disciplineMapper;
        this.calc = calc;
    }

    /** 获取或创建综测记录 */
    public EvaluationRecord findOrCreate(Long studentId, Long yearId) {
        EvaluationRecord r = evalMapper.selectOne(Wrappers.<EvaluationRecord>lambdaQuery()
                .eq(EvaluationRecord::getStudentId, studentId)
                .eq(EvaluationRecord::getAcademicYearId, yearId));
        if (r != null) return r;
        r = new EvaluationRecord();
        r.setStudentId(studentId);
        r.setAcademicYearId(yearId);
        r.setStatus("DRAFT");
        r.setMoralAppraisalScore(BigDecimal.ZERO);
        r.setMoralRecordScore(BigDecimal.ZERO);
        r.setMoralTotal(BigDecimal.ZERO);
        r.setAcademicWeightedAvg(BigDecimal.ZERO);
        r.setBasicTotal(BigDecimal.ZERO);
        r.setAbilityBase(75);
        r.setResearchInnovation(BigDecimal.ZERO);
        r.setProfessionalSkill(BigDecimal.ZERO);
        r.setOrganizationWork(BigDecimal.ZERO);
        r.setSportsAesthetics(BigDecimal.ZERO);
        r.setLaborPractice(BigDecimal.ZERO);
        r.setAbilityTotal(BigDecimal.ZERO);
        evalMapper.insert(r);
        return r;
    }

    // ===== 基本项计算 =====

    @Transactional
    public Map<String, Object> recalculateBasic(Long studentId, Long yearId) {
        EvaluationRecord rec = findOrCreate(studentId, yearId);

        // 品德评议分
        List<MoralAppraisal> appraisals = moralAppraisalMapper.selectList(
                Wrappers.<MoralAppraisal>lambdaQuery()
                        .eq(MoralAppraisal::getEvaluationId, rec.getId()));
        BigDecimal appraisalScore = calc.moralAppraisalScore(appraisals);

        // 品德记实分
        List<MoralRecordItem> recordItems = moralRecordMapper.selectList(
                Wrappers.<MoralRecordItem>lambdaQuery()
                        .eq(MoralRecordItem::getEvaluationId, rec.getId())
                        .ne(MoralRecordItem::getReviewStatus, "REJECTED"));
        BigDecimal recordScore = calc.moralRecordScore(recordItems);

        // 品德总分
        BigDecimal moralTotal = calc.moralTotal(appraisalScore, recordScore);

        // 专业素质（加权平均）
        List<CourseGrade> grades = courseGradeMapper.selectList(
                Wrappers.<CourseGrade>lambdaQuery()
                        .eq(CourseGrade::getStudentId, studentId)
                        .eq(CourseGrade::getAcademicYearId, yearId));
        BigDecimal academicAvg = calc.academicWeightedAvg(grades);

        // 基本项总分
        BigDecimal basicTotal = calc.basicTotal(moralTotal, academicAvg);

        rec.setMoralAppraisalScore(appraisalScore.setScale(2, RoundingMode.HALF_UP));
        rec.setMoralRecordScore(recordScore.setScale(2, RoundingMode.HALF_UP));
        rec.setMoralTotal(moralTotal);
        rec.setAcademicWeightedAvg(academicAvg.setScale(2, RoundingMode.HALF_UP));
        rec.setBasicTotal(basicTotal);
        evalMapper.updateById(rec);

        Map<String, Object> resp = new HashMap<>();
        resp.put("moralAppraisal", rec.getMoralAppraisalScore());
        resp.put("moralRecord", rec.getMoralRecordScore());
        resp.put("moralTotal", rec.getMoralTotal());
        resp.put("academicAvg", rec.getAcademicWeightedAvg());
        resp.put("basicTotal", rec.getBasicTotal());
        return resp;
    }

    // ===== 综合能力计算 =====

    @Transactional
    public Map<String, Object> recalculateAbility(Long studentId, Long yearId) {
        EvaluationRecord rec = findOrCreate(studentId, yearId);

        BigDecimal ri = sumResearchInnovation(rec.getId());
        BigDecimal ps = sumProfessionalSkill(rec.getId());
        BigDecimal ow = maxOrganizationWork(rec.getId());
        BigDecimal sa = sumSportsAesthetics(rec.getId());
        BigDecimal lp = sumLaborPractice(rec.getId());

        BigDecimal abilityTotal = calc.abilityTotal(ri, ps, ow, sa, lp);

        rec.setResearchInnovation(ri.setScale(2, RoundingMode.HALF_UP));
        rec.setProfessionalSkill(ps.setScale(2, RoundingMode.HALF_UP));
        rec.setOrganizationWork(ow.setScale(2, RoundingMode.HALF_UP));
        rec.setSportsAesthetics(sa.setScale(2, RoundingMode.HALF_UP));
        rec.setLaborPractice(lp.setScale(2, RoundingMode.HALF_UP));
        rec.setAbilityTotal(abilityTotal);
        evalMapper.updateById(rec);

        Map<String, Object> resp = new HashMap<>();
        resp.put("researchInnovation", rec.getResearchInnovation());
        resp.put("professionalSkill", rec.getProfessionalSkill());
        resp.put("organizationWork", rec.getOrganizationWork());
        resp.put("sportsAesthetics", rec.getSportsAesthetics());
        resp.put("laborPractice", rec.getLaborPractice());
        resp.put("abilityTotal", rec.getAbilityTotal());
        return resp;
    }

    @Transactional
    public void recalculateAll(Long studentId, Long yearId) {
        recalculateBasic(studentId, yearId);
        recalculateAbility(studentId, yearId);
    }

    // ===== 提交 =====

    @Transactional
    public void submit(Long studentId, Long yearId) {
        recalculateAll(studentId, yearId);
        EvaluationRecord rec = findOrCreate(studentId, yearId);
        rec.setStatus("SUBMITTED");
        rec.setSubmittedAt(LocalDateTime.now());
        evalMapper.updateById(rec);
    }

    // ===== 五模块求和 =====

    private BigDecimal sumResearchInnovation(Long evalId) {
        List<ResearchInnovationItem> items = riMapper.selectList(
                Wrappers.<ResearchInnovationItem>lambdaQuery()
                        .eq(ResearchInnovationItem::getEvaluationId, evalId)
                        .ne(ResearchInnovationItem::getReviewStatus, "REJECTED"));
        BigDecimal sum = BigDecimal.ZERO;
        for (ResearchInnovationItem item : items) {
            BigDecimal s = item.getScore() == null
                    ? calc.researchInnovation(item) : item.getScore();
            sum = sum.add(s);
        }
        return sum;
    }

    private BigDecimal sumProfessionalSkill(Long evalId) {
        List<ProfessionalSkillItem> items = psMapper.selectList(
                Wrappers.<ProfessionalSkillItem>lambdaQuery()
                        .eq(ProfessionalSkillItem::getEvaluationId, evalId)
                        .ne(ProfessionalSkillItem::getReviewStatus, "REJECTED"));
        BigDecimal sum = BigDecimal.ZERO;
        for (ProfessionalSkillItem item : items) {
            BigDecimal s = item.getScore() == null
                    ? calc.professionalSkill(item) : item.getScore();
            sum = sum.add(s);
        }
        return sum;
    }

    /** 组织工作：任多项职务者以最高职务类别计分 */
    private BigDecimal maxOrganizationWork(Long evalId) {
        List<OrganizationWorkItem> items = owMapper.selectList(
                Wrappers.<OrganizationWorkItem>lambdaQuery()
                        .eq(OrganizationWorkItem::getEvaluationId, evalId)
                        .ne(OrganizationWorkItem::getReviewStatus, "REJECTED"));
        BigDecimal max = BigDecimal.ZERO;
        for (OrganizationWorkItem item : items) {
            BigDecimal s = item.getScore() == null
                    ? calc.organizationWork(item) : item.getScore();
            if (s.compareTo(max) > 0) max = s;
        }
        return max;
    }

    private BigDecimal sumSportsAesthetics(Long evalId) {
        List<SportsAestheticsItem> items = saMapper.selectList(
                Wrappers.<SportsAestheticsItem>lambdaQuery()
                        .eq(SportsAestheticsItem::getEvaluationId, evalId)
                        .ne(SportsAestheticsItem::getReviewStatus, "REJECTED"));
        BigDecimal sum = BigDecimal.ZERO;
        for (SportsAestheticsItem item : items) {
            BigDecimal s = item.getScore() == null
                    ? calc.sportsAesthetics(item) : item.getScore();
            sum = sum.add(s);
        }
        return sum;
    }

    private BigDecimal sumLaborPractice(Long evalId) {
        List<LaborPracticeItem> items = lpMapper.selectList(
                Wrappers.<LaborPracticeItem>lambdaQuery()
                        .eq(LaborPracticeItem::getEvaluationId, evalId)
                        .ne(LaborPracticeItem::getReviewStatus, "REJECTED"));
        BigDecimal sum = BigDecimal.ZERO;
        for (LaborPracticeItem item : items) {
            BigDecimal s = item.getScore() == null
                    ? calc.laborPractice(item) : item.getScore();
            sum = sum.add(s);
        }
        return sum;
    }

    // ===== P0-1 新增：全科合格校验 =====

    /**
     * 检查学生该学年所有课程是否全部合格（≥60分）
     * @return 不合格的课程列表，空列表表示全部合格
     */
    public List<CourseGrade> checkAllCoursesPass(Long studentId, Long yearId) {
        List<CourseGrade> grades = courseGradeMapper.selectList(
                Wrappers.<CourseGrade>lambdaQuery()
                        .eq(CourseGrade::getStudentId, studentId)
                        .eq(CourseGrade::getAcademicYearId, yearId));
        if (grades == null || grades.isEmpty()) return Collections.emptyList();
        return grades.stream()
                .filter(g -> g.getScore() != null && g.getScore().compareTo(BigDecimal.valueOf(60)) < 0)
                .collect(Collectors.toList());
    }

    /**
     * 计算外语课加权平均分
     * 筛选课程名称包含"英语"/"外语"的课程
     */
    public BigDecimal calculateForeignLangAvg(Long studentId, Long yearId) {
        List<CourseGrade> grades = courseGradeMapper.selectList(
                Wrappers.<CourseGrade>lambdaQuery()
                        .eq(CourseGrade::getStudentId, studentId)
                        .eq(CourseGrade::getAcademicYearId, yearId));
        BigDecimal sumProduct = BigDecimal.ZERO;
        BigDecimal sumCredit = BigDecimal.ZERO;
        for (CourseGrade g : grades) {
            if (g.getScore() == null || g.getCredit() == null) continue;
            String name = g.getCourseName();
            if (name == null) continue;
            if (name.contains("英语") || name.contains("外语") || name.toLowerCase().contains("english")) {
                sumProduct = sumProduct.add(g.getScore().multiply(g.getCredit()));
                sumCredit = sumCredit.add(g.getCredit());
            }
        }
        if (sumCredit.compareTo(BigDecimal.ZERO) == 0) return null;
        return sumProduct.divide(sumCredit, 4, RoundingMode.HALF_UP);
    }

    /** 检查学生是否有未解除的处分 */
    public boolean hasUnresolvedDiscipline(Long studentId) {
        Long count = disciplineMapper.selectCount(
                Wrappers.<DisciplineRecord>lambdaQuery()
                        .eq(DisciplineRecord::getStudentId, studentId)
                        .eq(DisciplineRecord::getIsResolved, false));
        return count != null && count > 0;
    }
}
