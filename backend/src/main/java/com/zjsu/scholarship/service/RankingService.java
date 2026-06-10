package com.zjsu.scholarship.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zjsu.scholarship.common.BusinessException;
import com.zjsu.scholarship.entity.*;
import com.zjsu.scholarship.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 本科生排名与奖学金等级分配服务（2025版完整规则）
 * <p>
 * 算法依据：
 * - 《浙江工商大学奖学金实施办法》(2025版) 第十七条—第二十条
 * - 双排名：基本项排名 + 综合能力排名
 * - 等级评定依照综合能力排名先后确定
 * - 基本项排名前30%为资格门槛
 * - 一等追加基本项前15% + 能力项前30%校验
 * </p>
 */
@Service
public class RankingService {

    private final EvaluationRecordMapper evalMapper;
    private final ScholarshipProjectMapper projectMapper;
    private final ScholarshipLevelMapper levelMapper;
    private final ApplicationMapper applicationMapper;
    private final StudentMapper studentMapper;
    private final EvaluationService evaluationService;

    public RankingService(EvaluationRecordMapper evalMapper,
                          ScholarshipProjectMapper projectMapper,
                          ScholarshipLevelMapper levelMapper,
                          ApplicationMapper applicationMapper,
                          StudentMapper studentMapper,
                          EvaluationService evaluationService) {
        this.evalMapper = evalMapper;
        this.projectMapper = projectMapper;
        this.levelMapper = levelMapper;
        this.applicationMapper = applicationMapper;
        this.studentMapper = studentMapper;
        this.evaluationService = evaluationService;
    }

    /**
     * 执行双排名并分配奖学金等级（修正版算法）
     * <p>
     * 步骤：
     * 1. 全员重算综测分
     * 2. 双排名：basicRank, abilityRank
     * 3. 按基本项排名过滤（默认前30%）
     * 4. 按能力排名排序
     * 5. 一等：前3%，额外校验基本项前15% + 能力项前30%
     * 6. 二等：次6%
     * 7. 三等：次12%
     * </p>
     */
    @Transactional
    public Map<String, Object> rankAndAssign(Long projectId) {
        ScholarshipProject project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException("奖学金项目不存在");
        Long yearId = project.getAcademicYearId();

        // ---- 步骤1：全员重算 ----
        List<Student> allStudents = studentMapper.selectList(null);
        for (Student s : allStudents) {
            evaluationService.recalculateAll(s.getId(), yearId);
        }

        List<EvaluationRecord> recs = evalMapper.selectList(
                Wrappers.<EvaluationRecord>lambdaQuery()
                        .eq(EvaluationRecord::getAcademicYearId, yearId));

        int total = recs.size();
        if (total == 0) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("totalStudents", 0);
            resp.put("levels", Collections.emptyList());
            return resp;
        }

        // ---- 步骤2：双排名 ----
        // 基本项排名（降序）
        List<EvaluationRecord> sortedByBasic = new ArrayList<>(recs);
        sortedByBasic.sort(Comparator
                .comparing(EvaluationRecord::getBasicTotal, Comparator.nullsLast(Comparator.reverseOrder())));

        // 综合能力排名（降序）
        List<EvaluationRecord> sortedByAbility = new ArrayList<>(recs);
        sortedByAbility.sort(Comparator
                .comparing(EvaluationRecord::getAbilityTotal, Comparator.nullsLast(Comparator.reverseOrder())));

        for (int i = 0; i < recs.size(); i++) {
            sortedByBasic.get(i).setBasicRank(i + 1);
            sortedByAbility.get(i).setAbilityRank(i + 1);
        }
        for (EvaluationRecord r : recs) {
            evalMapper.updateById(r);
        }

        // ---- 步骤3：按基本项排名过滤 ----
        BigDecimal basicMaxRatio = project.getRankBasicMaxRatio() != null
                ? project.getRankBasicMaxRatio()
                : new BigDecimal("30"); // 默认前30%
        int basicPassCount = basicMaxRatio.multiply(BigDecimal.valueOf(total))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();

        Set<Long> eligibleStudentIds = new HashSet<>();
        for (EvaluationRecord r : sortedByBasic) {
            if (r.getBasicRank() != null && r.getBasicRank() <= basicPassCount) {
                eligibleStudentIds.add(r.getStudentId());
            }
        }

        // 按能力排名排序，但只取基本项合格的
        List<EvaluationRecord> eligibleByAbility = new ArrayList<>();
        for (EvaluationRecord r : sortedByAbility) {
            if (eligibleStudentIds.contains(r.getStudentId())) {
                eligibleByAbility.add(r);
            }
        }

        // ---- 步骤4-7：按比例切分等级 ----
        List<ScholarshipLevel> levels = levelMapper.selectList(
                Wrappers.<ScholarshipLevel>lambdaQuery()
                        .eq(ScholarshipLevel::getProjectId, projectId)
                        .orderByAsc(ScholarshipLevel::getLevelOrder));

        // 计算每等名额
        for (ScholarshipLevel lvl : levels) {
            if (lvl.getQuota() == null) {
                BigDecimal ratio = lvl.getRatio() == null ? BigDecimal.ZERO : lvl.getRatio();
                int q = ratio.multiply(BigDecimal.valueOf(total))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
                lvl.setQuota(q);
                levelMapper.updateById(lvl);
            }
        }

        // 分配等级：一等有额外校验（basicRank ≤ 15% AND abilityRank ≤ 30%）
        BigDecimal firstBasicRatio = project.getRankBasicFirst() != null
                ? project.getRankBasicFirst()
                : new BigDecimal("15");  // 默认基本项前15%
        BigDecimal firstAbilityRatio = project.getRankAbilityFirst() != null
                ? project.getRankAbilityFirst()
                : new BigDecimal("30");  // 默认能力项前30%
        int firstBasicLimit = firstBasicRatio.multiply(BigDecimal.valueOf(total))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
        int firstAbilityLimit = firstAbilityRatio.multiply(BigDecimal.valueOf(total))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();

        Map<Long, Long> studentToLevel = new HashMap<>(); // studentId -> levelId
        int cursor = 0;
        for (ScholarshipLevel lvl : levels) {
            int q = lvl.getQuota() == null ? 0 : lvl.getQuota();
            int assigned = 0;
            int scanned = 0;

            while (assigned < q && (cursor + scanned) < eligibleByAbility.size()) {
                EvaluationRecord r = eligibleByAbility.get(cursor + scanned);
                scanned++;

                // 一等额外校验
                if (lvl.getLevelOrder() != null && lvl.getLevelOrder() == 1) {
                    // 基本项排名 ≤ 15%
                    if (r.getBasicRank() != null && r.getBasicRank() > firstBasicLimit) {
                        continue;
                    }
                    // 能力项排名 ≤ 30%
                    if (r.getAbilityRank() != null && r.getAbilityRank() > firstAbilityLimit) {
                        continue;
                    }
                }

                // 该等级的额外排名限制
                if (lvl.getRankBasicMaxRatio() != null) {
                    int limit = lvl.getRankBasicMaxRatio().multiply(BigDecimal.valueOf(total))
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
                    if (r.getBasicRank() != null && r.getBasicRank() > limit) continue;
                }
                if (lvl.getRankAbilityMaxRatio() != null) {
                    int limit = lvl.getRankAbilityMaxRatio().multiply(BigDecimal.valueOf(total))
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
                    if (r.getAbilityRank() != null && r.getAbilityRank() > limit) continue;
                }

                studentToLevel.put(r.getStudentId(), lvl.getId());
                assigned++;
            }
            cursor += scanned;
        }

        // 写入推荐等级到已有申请记录
        List<Application> apps = applicationMapper.selectList(
                Wrappers.<Application>lambdaQuery()
                        .eq(Application::getProjectId, projectId));
        for (Application app : apps) {
            Long lvlId = studentToLevel.get(app.getStudentId());
            app.setAutoLevelId(lvlId);
            EvaluationRecord rec = evalMapper.selectOne(
                    Wrappers.<EvaluationRecord>lambdaQuery()
                            .eq(EvaluationRecord::getStudentId, app.getStudentId())
                            .eq(EvaluationRecord::getAcademicYearId, yearId));
            if (rec != null) {
                app.setSnapshotBasicTotal(rec.getBasicTotal());
                app.setSnapshotBasicRank(rec.getBasicRank());
                app.setSnapshotAbilityTotal(rec.getAbilityTotal());
                app.setSnapshotAbilityRank(rec.getAbilityRank());
            }
            applicationMapper.updateById(app);
        }

        project.setRanked(true);
        project.setStatus("REVIEWING");
        projectMapper.updateById(project);

        Map<String, Object> resp = new HashMap<>();
        resp.put("totalStudents", total);
        resp.put("eligibleCount", eligibleByAbility.size());
        resp.put("filteredOut", total - eligibleByAbility.size());
        resp.put("levels", levels);
        return resp;
    }

    /** 查询学生在某项目的推荐等级 */
    public Long previewLevelForStudent(Long studentId, Long projectId) {
        ScholarshipProject project = projectMapper.selectById(projectId);
        if (project == null) return null;
        Long yearId = project.getAcademicYearId();
        EvaluationRecord rec = evalMapper.selectOne(
                Wrappers.<EvaluationRecord>lambdaQuery()
                        .eq(EvaluationRecord::getStudentId, studentId)
                        .eq(EvaluationRecord::getAcademicYearId, yearId));
        if (rec == null || rec.getAbilityRank() == null) return null;

        int total = evalMapper.selectCount(
                Wrappers.<EvaluationRecord>lambdaQuery()
                        .eq(EvaluationRecord::getAcademicYearId, yearId)).intValue();

        // 基本项过滤
        BigDecimal basicMaxRatio = project.getRankBasicMaxRatio() != null
                ? project.getRankBasicMaxRatio() : new BigDecimal("30");
        int basicPassCount = basicMaxRatio.multiply(BigDecimal.valueOf(total))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
        if (rec.getBasicRank() != null && rec.getBasicRank() > basicPassCount) return null;

        List<ScholarshipLevel> levels = levelMapper.selectList(
                Wrappers.<ScholarshipLevel>lambdaQuery()
                        .eq(ScholarshipLevel::getProjectId, projectId)
                        .orderByAsc(ScholarshipLevel::getLevelOrder));

        BigDecimal firstBasicRatio = project.getRankBasicFirst() != null
                ? project.getRankBasicFirst() : new BigDecimal("15");
        BigDecimal firstAbilityRatio = project.getRankAbilityFirst() != null
                ? project.getRankAbilityFirst() : new BigDecimal("30");
        int firstBasicLimit = firstBasicRatio.multiply(BigDecimal.valueOf(total))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
        int firstAbilityLimit = firstAbilityRatio.multiply(BigDecimal.valueOf(total))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();

        int cursor = 0;
        for (ScholarshipLevel lvl : levels) {
            int q = lvl.getQuota() != null ? lvl.getQuota()
                    : (lvl.getRatio() == null ? 0
                    : lvl.getRatio().multiply(BigDecimal.valueOf(total))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                    .intValue());

            // 一等额外校验
            if (lvl.getLevelOrder() != null && lvl.getLevelOrder() == 1) {
                if (rec.getBasicRank() != null && rec.getBasicRank() > firstBasicLimit) {
                    cursor += q;
                    continue;
                }
                if (rec.getAbilityRank() != null && rec.getAbilityRank() > firstAbilityLimit) {
                    cursor += q;
                    continue;
                }
            }

            int upper = cursor + q;
            if (rec.getAbilityRank() <= upper && rec.getAbilityRank() > cursor) {
                return lvl.getId();
            }
            cursor = upper;
        }
        return null;
    }

    /**
     * 校验学生是否符合奖学金硬性条件（完整5条件版）
     * <p>
     * 条件一：所有课程全部合格(≥60)+加权平均分≥75
     * 条件二：基本项排名前30%（一等追加：基本项前15%+能力项前30%）
     * 条件三：外语课均分≥75(一等≥80)或CET4合格或托福/雅思替代
     * 条件四：体育≥80（免测除外）
     * 条件五：劳动教育合格
     * 附加：无未解除处分
     * </p>
     *
     * @param student   学生信息
     * @param rec       综测记录
     * @param project   奖学金项目
     * @param targetLevelId 目标等级ID（可选，用于一等特殊校验）
     * @return null 表示通过，否则返回失败原因
     */
    public String checkEligibility(Student student, EvaluationRecord rec,
                                    ScholarshipProject project, Long targetLevelId) {
        Long yearId = project.getAcademicYearId();

        // ---- 条件一：全部课程合格 + 加权平均≥75 ----
        List<CourseGrade> failedCourses = evaluationService.checkAllCoursesPass(student.getId(), yearId);
        if (!failedCourses.isEmpty()) {
            return "存在不合格课程：" + failedCourses.stream()
                    .map(c -> c.getCourseName() + "(" + c.getScore() + "分)")
                    .limit(3)
                    .reduce((a, b) -> a + "、" + b).orElse("");
        }

        if (project.getMinWeightedAvg() != null
                && rec.getAcademicWeightedAvg() != null
                && rec.getAcademicWeightedAvg().compareTo(project.getMinWeightedAvg()) < 0) {
            return "加权平均分未达标（" + rec.getAcademicWeightedAvg()
                    + " < " + project.getMinWeightedAvg() + "）";
        }

        // ---- 条件二：基本项排名前30% ----
        BigDecimal basicMaxRatio = project.getRankBasicMaxRatio() != null
                ? project.getRankBasicMaxRatio() : new BigDecimal("30");
        int totalStudents = evalMapper.selectCount(
                Wrappers.<EvaluationRecord>lambdaQuery()
                        .eq(EvaluationRecord::getAcademicYearId, yearId)).intValue();
        int basicThreshold = basicMaxRatio.multiply(BigDecimal.valueOf(totalStudents))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
        if (rec.getBasicRank() != null && rec.getBasicRank() > basicThreshold) {
            return "基本项排名未进入前" + basicMaxRatio + "%（排名：" + rec.getBasicRank()
                    + "，阈值：" + basicThreshold + "）";
        }

        // 一等追加校验
        if (targetLevelId != null) {
            ScholarshipLevel targetLevel = levelMapper.selectById(targetLevelId);
            if (targetLevel != null && targetLevel.getLevelOrder() != null && targetLevel.getLevelOrder() == 1) {
                BigDecimal firstBasicRatio = project.getRankBasicFirst() != null
                        ? project.getRankBasicFirst() : new BigDecimal("15");
                BigDecimal firstAbilityRatio = project.getRankAbilityFirst() != null
                        ? project.getRankAbilityFirst() : new BigDecimal("30");
                int firstBasicLimit = firstBasicRatio.multiply(BigDecimal.valueOf(totalStudents))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
                int firstAbilityLimit = firstAbilityRatio.multiply(BigDecimal.valueOf(totalStudents))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();

                if (rec.getBasicRank() != null && rec.getBasicRank() > firstBasicLimit) {
                    return "申请一等奖学金需基本项排名前" + firstBasicRatio + "%";
                }
                if (rec.getAbilityRank() != null && rec.getAbilityRank() > firstAbilityLimit) {
                    return "申请一等奖学金需综合能力排名前" + firstAbilityRatio + "%";
                }
            }
        }

        // ---- 条件三：外语 ----
        BigDecimal foreignLangAvg = evaluationService.calculateForeignLangAvg(student.getId(), yearId);
        boolean foreignOk = false;
        StringBuilder foreignDetail = new StringBuilder();

        // 方式1：外语课均分
        BigDecimal langAvgMin = project.getForeignLangAvgMin() != null
                ? project.getForeignLangAvgMin() : new BigDecimal("75");
        if (foreignLangAvg != null && foreignLangAvg.compareTo(langAvgMin) >= 0) {
            foreignOk = true;
            foreignDetail.append("外语课均分").append(foreignLangAvg.setScale(1, RoundingMode.HALF_UP)).append("≥").append(langAvgMin);
        }

        // 一等外语均分更高
        if (targetLevelId != null && foreignOk) {
            ScholarshipLevel targetLevel = levelMapper.selectById(targetLevelId);
            if (targetLevel != null && targetLevel.getLevelOrder() != null && targetLevel.getLevelOrder() == 1) {
                BigDecimal firstLangMin = project.getForeignLangAvgFirst() != null
                        ? project.getForeignLangAvgFirst() : new BigDecimal("80");
                if (foreignLangAvg != null && foreignLangAvg.compareTo(firstLangMin) < 0) {
                    foreignOk = false;
                    foreignDetail.append("（一等需≥").append(firstLangMin).append("，不满足）");
                }
            }
        }

        // 方式2：CET4合格（三/四年级）
        if (!foreignOk && Boolean.TRUE.equals(project.getRequireCet4Pass())) {
            if (student.getCet4Score() != null && student.getCet4Score() >= 425) {
                foreignOk = true;
                foreignDetail.append("CET4合格(").append(student.getCet4Score()).append("分)");
            } else if (student.getCet6Score() != null && student.getCet6Score() >= 425) {
                foreignOk = true;
                foreignDetail.append("CET6合格(").append(student.getCet6Score()).append("分)");
            }
        }

        if (!foreignOk) {
            return "外语条件未满足（" + foreignDetail + "）";
        }

        // ---- 条件四：体育≥80 ----
        if (project.getMinPeScore() != null
                && !Boolean.TRUE.equals(student.getPeExempt())) {
            if (student.getPeScore() == null
                    || student.getPeScore().compareTo(project.getMinPeScore()) < 0) {
                return "体育成绩未达标（" + student.getPeScore()
                        + " < " + project.getMinPeScore() + "）";
            }
        }

        // ---- 条件五：劳动教育合格 ----
        if (Boolean.TRUE.equals(project.getNeedLaborPass())
                && !"PASS".equals(student.getLaborEvaluation())) {
            return "劳动教育未达标";
        }

        // ---- 附加：无未解除处分 ----
        if (evaluationService.hasUnresolvedDiscipline(student.getId())) {
            return "存在未解除的处分记录";
        }

        return null; // 通过所有条件
    }

    /** 兼容旧接口的简单调用 */
    public String checkEligibility(Student student, EvaluationRecord rec,
                                    ScholarshipProject project) {
        return checkEligibility(student, rec, project, null);
    }
}
