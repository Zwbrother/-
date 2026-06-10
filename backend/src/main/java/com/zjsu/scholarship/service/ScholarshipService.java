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
 * 奖学金业务服务
 * <p>
 * 包含：能力突出奖学金自动判定、考研奖学金申报、申报限制校验、奖金发放规则
 * </p>
 */
@Service
public class ScholarshipService {

    /** 考研奖学金一等奖金额 */
    private static final BigDecimal GE_FIRST_AMOUNT = new BigDecimal("600");
    /** 考研奖学金二等奖金额 */
    private static final BigDecimal GE_SECOND_AMOUNT = new BigDecimal("300");

    private final StudentMapper studentMapper;
    private final AcademicYearMapper yearMapper;
    private final EvaluationRecordMapper evalMapper;
    private final ApplicationMapper applicationMapper;
    private final ScholarshipProjectMapper projectMapper;
    private final ScholarshipLevelMapper levelMapper;
    private final GraduateExamApplicationMapper graduateExamMapper;
    private final ResearchInnovationItemMapper riMapper;

    public ScholarshipService(StudentMapper studentMapper,
                               AcademicYearMapper yearMapper,
                               EvaluationRecordMapper evalMapper,
                               ApplicationMapper applicationMapper,
                               ScholarshipProjectMapper projectMapper,
                               ScholarshipLevelMapper levelMapper,
                               GraduateExamApplicationMapper graduateExamMapper,
                               ResearchInnovationItemMapper riMapper) {
        this.studentMapper = studentMapper;
        this.yearMapper = yearMapper;
        this.evalMapper = evalMapper;
        this.applicationMapper = applicationMapper;
        this.projectMapper = projectMapper;
        this.levelMapper = levelMapper;
        this.graduateExamMapper = graduateExamMapper;
        this.riMapper = riMapper;
    }

    // ============================================================
    //  能力突出奖学金自动判定（第十一条）
    // ============================================================

    /**
     * 能力突出奖学金资格判定结果
     */
    public static class AbilityEligibility {
        public boolean learningExcellence;      // 学习优秀奖
        public boolean abilityOutstanding;       // 综合能力突出奖
        public boolean researchInnovation;       // 研究创新奖
        public String learningExcellenceReason;
        public String abilityOutstandingReason;
        public String researchInnovationReason;
        public boolean alreadyHasComprehensive;  // 已获综合奖学金则不符合
        public String message;
    }

    /**
     * 在综合奖学金评定完成后，自动判定能力突出奖学金资格
     */
    public AbilityEligibility checkAbilityScholarship(Long studentId, Long yearId) {
        AbilityEligibility result = new AbilityEligibility();

        EvaluationRecord rec = evalMapper.selectOne(
                Wrappers.<EvaluationRecord>lambdaQuery()
                        .eq(EvaluationRecord::getStudentId, studentId)
                        .eq(EvaluationRecord::getAcademicYearId, yearId));
        if (rec == null) {
            result.message = "综测记录不存在";
            return result;
        }

        // 检查是否已获得综合奖学金
        ScholarshipProject compProject = projectMapper.selectOne(
                Wrappers.<ScholarshipProject>lambdaQuery()
                        .eq(ScholarshipProject::getAcademicYearId, yearId)
                        .eq(ScholarshipProject::getTypeCode, "COMPREHENSIVE"));
        if (compProject != null) {
            Application compApp = applicationMapper.selectOne(
                    Wrappers.<Application>lambdaQuery()
                            .eq(Application::getStudentId, studentId)
                            .eq(Application::getProjectId, compProject.getId())
                            .in(Application::getStatus, Arrays.asList("APPROVED", "PUBLISHED")));
            if (compApp != null && compApp.getFinalLevelId() != null) {
                result.alreadyHasComprehensive = true;
                result.message = "已获得优秀学生综合奖学金，不符合能力突出奖学金申请条件";
                return result;
            }
        }

        int total = evalMapper.selectCount(
                Wrappers.<EvaluationRecord>lambdaQuery()
                        .eq(EvaluationRecord::getAcademicYearId, yearId)).intValue();

        // 学习优秀奖：专业素质排名前20%，且未获综合奖学金
        if (rec.getBasicRank() != null) {
            int top20 = new BigDecimal("20").multiply(BigDecimal.valueOf(total))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
            if (rec.getBasicRank() <= top20) {
                result.learningExcellence = true;
                result.learningExcellenceReason = "专业素质排名第" + rec.getBasicRank() + "名（进入前20%）";
            }
        }

        // 综合能力突出奖：综合能力排名前20%，且未获综合奖学金
        if (rec.getAbilityRank() != null) {
            int top20 = new BigDecimal("20").multiply(BigDecimal.valueOf(total))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
            if (rec.getAbilityRank() <= top20) {
                result.abilityOutstanding = true;
                result.abilityOutstandingReason = "综合能力排名第" + rec.getAbilityRank() + "名（进入前20%）";
            }
        }

        // 研究创新奖：国家级三等/省级一等（排名前三）或 SCI/EI/ISTP 第一作者
        List<ResearchInnovationItem> riItems = riMapper.selectList(
                Wrappers.<ResearchInnovationItem>lambdaQuery()
                        .eq(ResearchInnovationItem::getEvaluationId, rec.getId())
                        .eq(ResearchInnovationItem::getReviewStatus, "APPROVED"));
        for (ResearchInnovationItem item : riItems) {
            if ("COMPETITION".equals(item.getItemType())) {
                String level = item.getLevelField();
                String award = item.getAwardLevel();
                Integer myRank = item.getMyRank();
                // 国家级三等或省级一等以上，排名前三
                if (("NATIONAL".equals(level) && "THIRD".equals(award) && (myRank == null || myRank <= 3))
                        || ("PROVINCIAL".equals(level) && ("FIRST".equals(award) || "SECOND".equals(award))
                            && (myRank == null || myRank <= 3))
                        || ("NATIONAL".equals(level) && ("FIRST".equals(award) || "SECOND".equals(award))
                            && (myRank == null || myRank <= 3))) {
                    result.researchInnovation = true;
                    result.researchInnovationReason = "学科竞赛" + level + " " + award + "，排名" + myRank;
                    break;
                }
            }
            if ("PAPER".equals(item.getItemType())) {
                String journal = item.getJournalLevel();
                Integer myRank = item.getMyRank();
                if (("SCI_Q1".equals(journal) || "SCI_Q2".equals(journal)
                        || "SCI_Q3".equals(journal) || "SCI_Q4".equals(journal))
                        && (myRank != null && myRank == 1)) {
                    result.researchInnovation = true;
                    result.researchInnovationReason = "SCI论文第一作者";
                    break;
                }
            }
        }

        if (!result.learningExcellence && !result.abilityOutstanding && !result.researchInnovation) {
            result.message = "暂不满足能力突出奖学金任一子类型条件";
        } else {
            List<String> types = new ArrayList<>();
            if (result.learningExcellence) types.add("学习优秀奖");
            if (result.abilityOutstanding) types.add("综合能力突出奖");
            if (result.researchInnovation) types.add("研究创新奖");
            result.message = "可申报：" + String.join("、", types) + "（每人限报一项）";
        }

        return result;
    }

    // ============================================================
    //  考研奖学金（第十二条）
    // ============================================================

    /** 提交考研奖学金申报 */
    @Transactional
    public GraduateExamApplication submitGraduateExam(Long studentId, Long yearId,
                                                       GraduateExamApplication app) {
        // ---- 校验：附件必传 ----
        if (app.getAttachmentUrl() == null || app.getAttachmentUrl().isBlank()) {
            throw new RuntimeException("请上传证明材料");
        }
        // ---- 校验：至少满足复试资格或已录取其中一项 ----
        if (!Boolean.TRUE.equals(app.getIsAdmitted())
                && !Boolean.TRUE.equals(app.getHasInterviewQualification())) {
            throw new RuntimeException("至少需要满足复试资格或已录取其中一个条件才能申报考研奖学金");
        }
        // ---- 校验：已录取必须同时具有复试资格 ----
        if (Boolean.TRUE.equals(app.getIsAdmitted())
                && !Boolean.TRUE.equals(app.getHasInterviewQualification())) {
            throw new RuntimeException("已录取的学生必须具有复试资格，请同时勾选复试资格");
        }

        GraduateExamApplication exist = graduateExamMapper.selectOne(
                Wrappers.<GraduateExamApplication>lambdaQuery()
                        .eq(GraduateExamApplication::getStudentId, studentId)
                        .eq(GraduateExamApplication::getAcademicYearId, yearId));

        // 已提交的不允许重复申报；退回/撤回/已通过的可重新提交
        if (exist != null && "SUBMITTED".equals(exist.getStatus())) {
            throw new RuntimeException("本学年已提交考研奖学金申报，请等待审核");
        }

        // 被退回、已撤回或已通过后可重新申报：更新现有记录
        if (exist != null) {
            exist.setExamType(app.getExamType());
            exist.setSchoolName(app.getSchoolName());
            exist.setMajorName(app.getMajorName());
            exist.setHasInterviewQualification(app.getHasInterviewQualification());
            exist.setIsAdmitted(app.getIsAdmitted());
            exist.setAttachmentUrl(app.getAttachmentUrl());
            exist.setStatus("SUBMITTED");
            exist.setFinalLevel(null);
            exist.setRejectReason(null);
            exist.setSubmittedAt(LocalDateTime.now());
            // 重新自动分级
            if (Boolean.TRUE.equals(exist.getIsAdmitted())) {
                exist.setFinalLevel("FIRST");
            } else if (Boolean.TRUE.equals(exist.getHasInterviewQualification())) {
                exist.setFinalLevel("SECOND");
            }
            graduateExamMapper.updateById(exist);
            return exist;
        }

        app.setStudentId(studentId);
        app.setAcademicYearId(yearId);
        app.setStatus("SUBMITTED");
        app.setSubmittedAt(LocalDateTime.now());

        // 自动分级
        if (Boolean.TRUE.equals(app.getIsAdmitted())) {
            app.setFinalLevel("FIRST");
        } else if (Boolean.TRUE.equals(app.getHasInterviewQualification())) {
            app.setFinalLevel("SECOND");
        }

        graduateExamMapper.insert(app);
        return app;
    }

    /** 获取学生考研奖学金申报状态 */
    public GraduateExamApplication getGraduateExamStatus(Long studentId, Long yearId) {
        return graduateExamMapper.selectOne(
                Wrappers.<GraduateExamApplication>lambdaQuery()
                        .eq(GraduateExamApplication::getStudentId, studentId)
                        .eq(GraduateExamApplication::getAcademicYearId, yearId));
    }

    // ============================================================
    //  申报限制校验（第十八条）
    // ============================================================

    /**
     * 检查申报限制：
     * - 能力突出奖学金每人限报一项
     * - 专项奖学金每人限报一项
     * - 单项奖学金每人限报一项
     * - 优秀学生综合奖学金不受此限
     */
    public String checkApplicationLimit(Long studentId, String category, Long yearId) {
        // 综合奖学金不限制
        if ("COMPREHENSIVE".equals(category)) return null;

        // 查询该学生该类别下所有非撤回的申请，再按学年过滤
        List<Application> existingList = applicationMapper.selectList(
                Wrappers.<Application>lambdaQuery()
                        .eq(Application::getStudentId, studentId)
                        .eq(Application::getApplicationCategory, category)
                        .ne(Application::getStatus, "WITHDRAWN"));

        for (Application existing : existingList) {
            ScholarshipProject p = projectMapper.selectById(existing.getProjectId());
            if (p != null && p.getAcademicYearId().equals(yearId)) {
                String pName = p.getProjectName();
                return "您已申报" + pName + "（" + categoryLabel(category)
                        + "），每人限报其中一项。请先撤回已提交的申请。";
            }
        }
        return null; // 通过
    }

    private String categoryLabel(String category) {
        return switch (category) {
            case "ABILITY" -> "能力突出奖学金";
            case "SPECIAL" -> "单项奖学金";
            case "NAMED" -> "专项奖学金";
            case "GRADUATE_EXAM" -> "考研奖学金";
            default -> category;
        };
    }

    // ============================================================
    //  奖金发放规则（第二十四条）
    // ============================================================

    /**
     * 计算学生实际应发奖金
     * 规则：综合奖学金与单项奖学金荣誉可同时授予，但奖金按额度高的一项发放
     */
    public BigDecimal calculateActualAmount(Long studentId, Long yearId) {
        // 常规奖学金
        List<Application> apps = applicationMapper.selectList(
                Wrappers.<Application>lambdaQuery()
                        .eq(Application::getStudentId, studentId)
                        .in(Application::getStatus, Arrays.asList("APPROVED", "PUBLISHED")));

        BigDecimal maxAmount = BigDecimal.ZERO;
        for (Application app : apps) {
            Long levelId = app.getFinalLevelId() != null ? app.getFinalLevelId() : app.getAutoLevelId();
            if (levelId != null) {
                ScholarshipLevel level = levelMapper.selectById(levelId);
                if (level != null && level.getAmount() != null
                        && level.getAmount().compareTo(maxAmount) > 0) {
                    maxAmount = level.getAmount();
                }
            }
        }

        // 考研奖学金
        GraduateExamApplication geApp = graduateExamMapper.selectOne(
                Wrappers.<GraduateExamApplication>lambdaQuery()
                        .eq(GraduateExamApplication::getStudentId, studentId)
                        .eq(GraduateExamApplication::getAcademicYearId, yearId)
                        .in(GraduateExamApplication::getStatus, Arrays.asList("APPROVED", "PUBLISHED")));
        if (geApp != null && geApp.getFinalLevel() != null) {
            BigDecimal geAmount = "FIRST".equals(geApp.getFinalLevel()) ? GE_FIRST_AMOUNT : GE_SECOND_AMOUNT;
            if (geAmount.compareTo(maxAmount) > 0) {
                maxAmount = geAmount;
            }
        }

        return maxAmount;
    }
}
