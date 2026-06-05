package com.zjsu.scholarship.service;

import java.math.BigDecimal;

/** 多人成果分摊系数表 */
public class CollabCoefficient {

    private static final BigDecimal[][] TABLE = new BigDecimal[][]{
            { bd("1.00") },
            { bd("0.60"), bd("0.40") },
            { bd("0.50"), bd("0.30"), bd("0.20") },
            { bd("0.40"), bd("0.30"), bd("0.20"), bd("0.10") },
            { bd("0.40"), bd("0.30"), bd("0.15"), bd("0.10"), bd("0.05") },
            { bd("0.40"), bd("0.30"), bd("0.15"), bd("0.07"), bd("0.04"), bd("0.04") }
    };

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    public static BigDecimal of(int totalAuthors, int rank) {
        if (totalAuthors < 1) totalAuthors = 1;
        if (rank < 1) rank = 1;
        int idx = Math.min(totalAuthors, 6) - 1;
        BigDecimal[] row = TABLE[idx];
        int rIdx = Math.min(rank, row.length) - 1;
        return row[rIdx];
    }
}
