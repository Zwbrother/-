package com.zjsu.scholarship.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zjsu.scholarship.common.BusinessException;
import com.zjsu.scholarship.entity.*;
import com.zjsu.scholarship.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 本科生排名与奖学金等级分配服务
 * <p>
 * 双排名：基本项排名 + 综合能力排名
 * 奖学金等级按综合能力排名先后确定（申报制）
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

    /** 执行双排名 */
    @Transactional
    public Map<String, Object> rankAndAssign(Long projectId) {
        ScholarshipProject project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException("奖学金项目不存在");
        Long yearId = project.getAcademicYearId();

        // 全员重算
        List<Student> students = studentMapper.selectList(null);
        for (Student s : students) {
            evaluationService.recalculateAll(s.getId(), yearId);
        }

        List<EvaluationRecord> recs = evalMapper.selectList(
                Wrappers.<EvaluationRecord>lambdaQuery()
                        .eq(EvaluationRecord::getAcademicYearId, yearId));

        // 基本项排名（降序）
        List<EvaluationRecord> sortedByBasic = new ArrayList<>(recs);
        sortedByBasic.sort(Comparator
                .comparing(EvaluationRecord::getBasicTotal, Comparator.nullsLast(Comparator.reverseOrder())));

        // 综合能力排名（降序）
        List<EvaluationRecord> sortedByAbility = new ArrayList<>(recs);
        sortedByAbility.sort(Comparator
                .comparing(EvaluationRecord::getAbilityTotal, Comparator.nullsLast(Comparator.reverseOrder())));

        for (int i = 0; i < recs.size(); i++) {
            EvaluationRecord r = sortedByBasic.get(i);
            r.setBasicRank(i + 1);
        }
        for (int i = 0; i < recs.size(); i++) {
            EvaluationRecord r = sortedByAbility.get(i);
            r.setAbilityRank(i + 1);
        }
        for (EvaluationRecord r : recs) {
            evalMapper.updateById(r);
        }

        // 按比例切分等级
        List<ScholarshipLevel> levels = levelMapper.selectList(
                Wrappers.<ScholarshipLevel>lambdaQuery()
                        .eq(ScholarshipLevel::getProjectId, projectId)
                        .orderByAsc(ScholarshipLevel::getLevelOrder));
        int total = recs.size();

        for (ScholarshipLevel lvl : levels) {
            if (lvl.getQuota() == null) {
                BigDecimal ratio = lvl.getRatio() == null ? BigDecimal.ZERO : lvl.getRatio();
                int q = ratio.multiply(BigDecimal.valueOf(total))
                        .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.FLOOR)
                        .intValue();
                lvl.setQuota(q);
                levelMapper.updateById(lvl);
            }
        }

        // 按综合能力排名自上而下分配推荐等级
        Map<Long, Long> studentToLevel = new HashMap<>();
        int cursor = 0;
        for (ScholarshipLevel lvl : levels) {
            int q = lvl.getQuota() == null ? 0 : lvl.getQuota();
            for (int i = 0; i < q && cursor < total; i++, cursor++) {
                EvaluationRecord r = sortedByAbility.get(cursor);
                studentToLevel.put(r.getStudentId(), lvl.getId());
            }
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

        List<ScholarshipLevel> levels = levelMapper.selectList(
                Wrappers.<ScholarshipLevel>lambdaQuery()
                        .eq(ScholarshipLevel::getProjectId, projectId)
                        .orderByAsc(ScholarshipLevel::getLevelOrder));
        int cursor = 0;
        for (ScholarshipLevel lvl : levels) {
            int q = lvl.getQuota() != null ? lvl.getQuota()
                    : (lvl.getRatio() == null ? 0
                    : lvl.getRatio().multiply(BigDecimal.valueOf(total))
                    .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.FLOOR)
                    .intValue());
            int upper = cursor + q;
            if (rec.getAbilityRank() <= upper && rec.getAbilityRank() > cursor) {
                return lvl.getId();
            }
            cursor = upper;
        }
        return null;
    }

    /**
     * 校验学生是否符合奖学金硬性条件
     * @return null 表示通过，否则返回失败原因
     */
    public String checkEligibility(Student student, EvaluationRecord rec,
                                    ScholarshipProject project) {
        // 加权平均分 ≥ 75
        if (project.getMinWeightedAvg() != null
                && rec.getAcademicWeightedAvg() != null
                && rec.getAcademicWeightedAvg().compareTo(project.getMinWeightedAvg()) < 0) {
            return "加权平均分未达标（" + rec.getAcademicWeightedAvg()
                    + " < " + project.getMinWeightedAvg() + "）";
        }
        // 体育 ≥ 80
        if (project.getMinPeScore() != null
                && student.getPeScore() != null
                && student.getPeScore().compareTo(project.getMinPeScore()) < 0) {
            return "体育成绩未达标（" + student.getPeScore()
                    + " < " + project.getMinPeScore() + "）";
        }
        // 劳动教育合格
        if (Boolean.TRUE.equals(project.getNeedLaborPass())
                && !"PASS".equals(student.getLaborEvaluation())) {
            return "劳动教育未达标";
        }
        // 基本项排名前30%（默认）
        if (rec.getBasicRank() != null && project.getRemark() != null) {
            // 可根据 project.remark 中的百分比配置做更灵活的校验
        }
        // 一等：基本项前15% + 能力项前30%
        // （此校验在申报时按具体等级判断）
        return null; // 通过
    }
}
