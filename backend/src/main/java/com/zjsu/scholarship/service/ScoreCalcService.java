package com.zjsu.scholarship.service;

import com.zjsu.scholarship.entity.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 本科生综测评审计分引擎（2025版规则）
 * <p>
 * 依据：
 * - 《浙江工商大学学生素质评价办法》(2025版)
 * - 《浙江工商大学奖学金实施办法》(2025版)
 * </p>
 */
@Service
public class ScoreCalcService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    // ============================================================
    //  第一部分：基本项 = 品德 × 30% + 专业素质 × 70%
    // ============================================================

    /** 品德评议分 = 自评×5% + 学生代表×60% + 辅导员(班主任)×35% */
    public BigDecimal moralAppraisalScore(List<MoralAppraisal> appraisals) {
        if (appraisals == null || appraisals.isEmpty()) return ZERO;
        BigDecimal self = ZERO;
        BigDecimal rep = ZERO;
        BigDecimal counselor = ZERO;
        for (MoralAppraisal a : appraisals) {
            BigDecimal total = sumSixDimensions(a);
            switch (a.getAppraiserType()) {
                case "SELF" -> self = total;
                case "STUDENT_REP" -> rep = total;
                case "COUNSELOR" -> counselor = total;
            }
        }
        return self.multiply(new BigDecimal("0.05"))
                .add(rep.multiply(new BigDecimal("0.60")))
                .add(counselor.multiply(new BigDecimal("0.35")))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumSixDimensions(MoralAppraisal a) {
        return nz(a.getPoliticalLiteracy())
                .add(nz(a.getLegalAwareness()))
                .add(nz(a.getMentalQuality()))
                .add(nz(a.getIntegrityScore()))
                .add(nz(a.getTeamwork()))
                .add(nz(a.getSocialResponsibility()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 品德记实分 = 基准60分 + 荣誉加分 - 处分扣分
     * 荣誉：国家级20/省级15/市级12/校级8/院级5
     * 处分：违法-10~-30/留校察看-10/记过-8~-10/严重警告-4~-6/警告-2~-4/通报批评-0.5~-2
     * 院班活动总加分不超过20分
     * 集体荣誉加分累计不超过20分
     */
    public BigDecimal moralRecordScore(List<MoralRecordItem> items) {
        BigDecimal base = BigDecimal.valueOf(60);
        BigDecimal honorPlus = ZERO;
        BigDecimal collectivePlus = ZERO;
        BigDecimal penalty = ZERO;

        for (MoralRecordItem item : items) {
            if (item.getScore() != null) {
                // 已经预计算过了
                BigDecimal s = item.getScore();
                if (s.compareTo(ZERO) > 0) {
                    honorPlus = honorPlus.add(s);
                } else {
                    penalty = penalty.add(s);
                }
                continue;
            }
            switch (item.getItemType()) {
                case "VOLUNTEER" -> {
                    BigDecimal hours = nz(item.getHours());
                    int fullSessions = hours.divide(BigDecimal.valueOf(4), 0, RoundingMode.FLOOR).intValue();
                    BigDecimal extra = hours.remainder(BigDecimal.valueOf(4)).compareTo(ZERO) > 0
                            ? BigDecimal.valueOf(2) : ZERO;
                    BigDecimal v = BigDecimal.valueOf(fullSessions * 4L).add(extra);
                    honorPlus = honorPlus.add(v.min(BigDecimal.TEN));
                }
                case "DISCIPLINE" -> {
                    int code = item.getRawValue() == null ? 0 : item.getRawValue().intValue();
                    int p = switch (code) {
                        case 1 -> 2;   // 通报批评
                        case 2 -> 4;   // 警告
                        case 3 -> 6;   // 严重警告
                        case 4 -> 10;  // 记过
                        case 5 -> 10;  // 留校察看
                        case 6 -> 30;  // 违法
                        default -> 0;
                    };
                    penalty = penalty.subtract(BigDecimal.valueOf(p));
                }
                case "HONOR" -> {
                    String level = nullSafe(item.getDescription());
                    BigDecimal h = switch (level) {
                        case "NATIONAL" -> BigDecimal.valueOf(20);
                        case "PROVINCIAL" -> BigDecimal.valueOf(15);
                        case "CITY" -> BigDecimal.valueOf(12);
                        case "SCHOOL" -> BigDecimal.valueOf(8);
                        case "COLLEGE" -> BigDecimal.valueOf(5);
                        default -> ZERO;
                    };
                    honorPlus = honorPlus.add(h);
                }
                case "COLLECTIVE_HONOR" -> {
                    BigDecimal c = switch (nullSafe(item.getDescription())) {
                        case "优良学风班" -> BigDecimal.valueOf(5);
                        case "学风特优班" -> BigDecimal.valueOf(10);
                        case "先进团支部" -> BigDecimal.valueOf(5);
                        case "五四团支部" -> BigDecimal.valueOf(8);
                        default -> ZERO;
                    };
                    collectivePlus = collectivePlus.add(c);
                }
            }
        }
        honorPlus = honorPlus.min(BigDecimal.valueOf(20)); // 院班活动上限
        collectivePlus = collectivePlus.min(BigDecimal.valueOf(20)); // 集体荣誉上限
        return base.add(honorPlus).add(collectivePlus).add(penalty)
                .max(ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /** 品德总分 = 评议分 × 70% + 记实分 × 30% */
    public BigDecimal moralTotal(BigDecimal appraisal, BigDecimal record) {
        return appraisal.multiply(new BigDecimal("0.70"))
                .add(record.multiply(new BigDecimal("0.30")))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** 专业素质：所有课程加权平均分 */
    public BigDecimal academicWeightedAvg(List<CourseGrade> grades) {
        if (grades == null || grades.isEmpty()) return ZERO;
        BigDecimal sumProduct = ZERO;
        BigDecimal sumCredit = ZERO;
        for (CourseGrade g : grades) {
            if (g.getScore() == null || g.getCredit() == null) continue;
            sumProduct = sumProduct.add(g.getScore().multiply(g.getCredit()));
            sumCredit = sumCredit.add(g.getCredit());
        }
        if (sumCredit.compareTo(ZERO) == 0) return ZERO;
        return sumProduct.divide(sumCredit, 4, RoundingMode.HALF_UP);
    }

    /** 基本项总分 = 品德总分 × 30% + 专业素质 × 70% */
    public BigDecimal basicTotal(BigDecimal moralTotal, BigDecimal academicAvg) {
        return moralTotal.multiply(new BigDecimal("0.30"))
                .add(academicAvg.multiply(new BigDecimal("0.70")))
                .setScale(3, RoundingMode.HALF_UP);
    }

    // ============================================================
    //  第二部分：综合能力 = 75 + 五模块加权
    // ============================================================

    /** 研究创新计分-单项 */
    public BigDecimal researchInnovation(ResearchInnovationItem item) {
        if (item == null || item.getItemType() == null) return ZERO;
        BigDecimal base;
        BigDecimal categoryCoef = ONE;

        switch (item.getItemType()) {
            case "COMPETITION" -> {
                base = BigDecimal.valueOf(compPoint(item.getLevelField(), item.getAwardLevel()));
                categoryCoef = switch (nullSafe(item.getCompetitionCategory())) {
                    case "A" -> new BigDecimal("1.2");
                    case "B" -> ONE;
                    case "C" -> new BigDecimal("0.5");
                    default -> ONE;
                };
            }
            case "PAPER" -> {
                base = switch (nullSafe(item.getJournalLevel())) {
                    case "FIRST_CLASS" -> BigDecimal.valueOf(50);
                    case "SECOND_CLASS" -> BigDecimal.valueOf(25);
                    case "THIRD_CLASS" -> BigDecimal.valueOf(15);
                    case "SCI_Q1" -> BigDecimal.valueOf(80);
                    case "SCI_Q2" -> BigDecimal.valueOf(60);
                    case "SCI_Q3" -> BigDecimal.valueOf(40);
                    case "SCI_Q4" -> BigDecimal.valueOf(30);
                    case "CSSCI" -> BigDecimal.valueOf(40);
                    default -> ZERO;
                };
            }
            case "PATENT" -> {
                base = switch (nullSafe(item.getPatentType())) {
                    case "INVENTION" -> BigDecimal.valueOf(50);
                    case "UTILITY" -> BigDecimal.valueOf(17);
                    case "APPEARANCE" -> BigDecimal.valueOf(13);
                    default -> ZERO;
                };
            }
            case "PROJECT" -> {
                String level = nullSafe(item.getProjectLevel());
                String status = nullSafe(item.getProjectStatus());
                int[] scores = switch (level) {
                    case "SCHOOL_GENERAL" -> new int[]{1, 2, -3};
                    case "SCHOOL_KEY" -> new int[]{2, 3, -5};
                    case "PROVINCIAL" -> new int[]{10, 10, -20};
                    case "NATIONAL" -> new int[]{10, 25, -35};
                    default -> new int[]{0, 0, 0};
                };
                int val = switch (status) {
                    case "APPROVED" -> scores[0];
                    case "CONCLUDED" -> scores[0] + scores[1];
                    case "OVERDUE" -> scores[2];
                    default -> 0;
                };
                base = BigDecimal.valueOf(val);
            }
            default -> { return ZERO; }
        }

        // 合作系数（多人成果）
        int total = item.getTotalAuthors() == null ? 1 : item.getTotalAuthors();
        int rank = item.getMyRank() == null ? 1 : item.getMyRank();
        if (Boolean.TRUE.equals(item.getHasAdvisor())) {
            total = Math.max(1, total - 1);
            rank = Math.max(1, rank - 1);
        }
        BigDecimal collab = ONE;
        if (total > 1) {
            collab = CollabCoefficient.of(total, rank);
        }
        // 核心成员系数
        if (Boolean.TRUE.equals(item.getIsCoreMember()) && total > 1) {
            collab = collab.min(new BigDecimal("0.8"));
        } else if (!Boolean.TRUE.equals(item.getIsCoreMember()) && total > 1) {
            collab = collab.min(new BigDecimal("0.5"));
        }

        return base.multiply(categoryCoef).multiply(collab)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** 研究创新-竞赛基础分对照表 (PDF 规则) */
    private int compPoint(String level, String award) {
        int row = switch (nullSafe(level)) {
            case "NATIONAL" -> 0;
            case "PROVINCIAL" -> 1;
            case "CITY" -> 2;
            case "SCHOOL" -> 3;
            case "COLLEGE" -> 4;
            default -> -1;
        };
        if (row < 0) return 0;
        int[][] base = {
                {50, 35, 30},  // NATIONAL   一等/二等/三等
                {30, 20, 15},  // PROVINCIAL
                {20, 15, 10},  // CITY
                {12, 8, 5},    // SCHOOL
                {5, 3, 2}      // COLLEGE
        };
        int col = switch (nullSafe(award)) {
            case "FIRST" -> 0;
            case "SECOND" -> 1;
            case "THIRD" -> 2;
            default -> -1;
        };
        if (col < 0) return 0;
        return base[row][col];
    }

    /** 专业技能计分-单项 */
    public BigDecimal professionalSkill(ProfessionalSkillItem item) {
        if (item == null || item.getItemType() == null) return ZERO;
        return switch (item.getItemType()) {
            case "CET4" -> {
                int score = item.getSkillCategory() != null
                        ? Integer.parseInt(item.getSkillCategory()) : 0;
                yield score >= 550 ? BigDecimal.valueOf(10)
                        : score >= 425 ? BigDecimal.valueOf(6)
                        : ZERO;
            }
            case "CET6" -> {
                int score = item.getSkillCategory() != null
                        ? Integer.parseInt(item.getSkillCategory()) : 0;
                yield score >= 520 ? BigDecimal.valueOf(15)
                        : score >= 425 ? BigDecimal.valueOf(12)
                        : ZERO;
            }
            case "COMPUTER" -> switch (nullSafe(item.getSkillLevel())) {
                case "LEVEL3" -> BigDecimal.valueOf(10);
                case "LEVEL2" -> BigDecimal.valueOf(6);
                default -> ZERO;
            };
            case "CERTIFICATE" -> switch (nullSafe(item.getSkillLevel())) {
                case "HIGH" -> BigDecimal.valueOf(30);
                case "MEDIUM" -> BigDecimal.valueOf(20);
                case "PRIMARY" -> BigDecimal.valueOf(10);
                default -> ZERO;
            };
            case "ENTRANCE_EXAM" -> BigDecimal.valueOf(12);
            default -> ZERO;
        };
    }

    /** 组织工作计分-单项 = (岗位分 + 绩效分) × 任期系数 */
    public BigDecimal organizationWork(OrganizationWorkItem item) {
        if (item == null) return ZERO;
        int position = item.getPositionScore() == null ? 0 : item.getPositionScore();
        int performance = switch (nullSafe(item.getPerformanceGrade())) {
            case "EXCELLENT" -> 8;
            case "COMPETENT" -> 2;
            case "INCOMPETENT" -> 0;
            default -> 0;
        };
        int months = item.getDurationMonths() == null ? 0 : item.getDurationMonths();
        BigDecimal coeff = months < 6 ? ZERO
                : months < 12 ? new BigDecimal("0.5") : ONE;
        if (performance == 0) return ZERO; // 不称职=0
        return BigDecimal.valueOf(position + performance)
                .multiply(coeff).setScale(2, RoundingMode.HALF_UP);
    }

    /** 体育美育计分-单项 */
    public BigDecimal sportsAesthetics(SportsAestheticsItem item) {
        if (item == null || item.getLevelField() == null) return ZERO;
        BigDecimal base = BigDecimal.valueOf(sportsLaborCompPoint(item.getLevelField(), item.getAwardLevel()));
        // 团队系数
        if (Boolean.TRUE.equals(item.getIsTeam())) {
            if (Boolean.TRUE.equals(item.getIsCoreMember())) {
                base = base.multiply(new BigDecimal("0.8"));
            } else {
                base = base.multiply(new BigDecimal("0.5"));
            }
        }
        return base.setScale(2, RoundingMode.HALF_UP);
    }

    /** 劳动教育和社会实践计分-单项 */
    public BigDecimal laborPractice(LaborPracticeItem item) {
        if (item == null || item.getLevelField() == null) return ZERO;
        BigDecimal base = BigDecimal.valueOf(sportsLaborCompPoint(item.getLevelField(), item.getAwardLevel()));
        if (Boolean.TRUE.equals(item.getIsTeam())) {
            if (Boolean.TRUE.equals(item.getIsCoreMember())) {
                base = base.multiply(new BigDecimal("0.8"));
            } else {
                base = base.multiply(new BigDecimal("0.5"));
            }
        }
        return base.setScale(2, RoundingMode.HALF_UP);
    }

    /** 体育美育/劳动教育 竞赛基础分对照表 */
    private int sportsLaborCompPoint(String level, String award) {
        int row = switch (nullSafe(level)) {
            case "NATIONAL" -> 0;
            case "PROVINCIAL" -> 1;
            case "CITY" -> 2;
            case "SCHOOL" -> 3;
            case "COLLEGE" -> 4;
            default -> -1;
        };
        if (row < 0) return 0;
        int[][] base = {
                {50, 35, 25},  // NATIONAL
                {30, 20, 15},  // PROVINCIAL
                {18, 12, 8},   // CITY
                {12, 8, 5},    // SCHOOL
                {5, 3, 2}      // COLLEGE
        };
        int col = switch (nullSafe(award)) {
            case "FIRST" -> 0;
            case "SECOND" -> 1;
            case "THIRD" -> 2;
            default -> -1;
        };
        if (col < 0) return 0;
        return base[row][col];
    }

    /**
     * 综合能力总分 = 75 + 研究创新×30% + 专业技能×25% + 组织工作×15%
     *                 + 体育美育×15% + 劳动教育和社会实践×15%
     */
    public BigDecimal abilityTotal(BigDecimal ri, BigDecimal ps, BigDecimal ow,
                                    BigDecimal sa, BigDecimal lp) {
        BigDecimal sum = ZERO;
        sum = sum.add(nz(ri).multiply(new BigDecimal("0.30")));
        sum = sum.add(nz(ps).multiply(new BigDecimal("0.25")));
        sum = sum.add(nz(ow).multiply(new BigDecimal("0.15")));
        sum = sum.add(nz(sa).multiply(new BigDecimal("0.15")));
        sum = sum.add(nz(lp).multiply(new BigDecimal("0.15")));
        return BigDecimal.valueOf(75).add(sum).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private BigDecimal nz(BigDecimal v) { return v == null ? ZERO : v; }
    private String nullSafe(String s) { return s == null ? "" : s; }
}
