package com.zjsu.scholarship.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zjsu.scholarship.entity.*;
import com.zjsu.scholarship.mapper.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ImportService {

    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final CourseGradeMapper courseGradeMapper;
    private final PasswordEncoder passwordEncoder;

    public ImportService(UserMapper userMapper, StudentMapper studentMapper,
                         CourseGradeMapper courseGradeMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.studentMapper = studentMapper;
        this.courseGradeMapper = courseGradeMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // ===== 模板下载 =====
    public void generateStudentTemplate(HttpServletResponse response) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("学生名单");
            String[] headers = {"学号*", "姓名*", "性别", "学院", "专业*", "年级", "班级", "宿舍号", "CET4成绩", "CET6成绩", "体育成绩", "劳动教育(PASS/PENDING)"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            Row demo = sheet.createRow(1);
            demo.createCell(0).setCellValue("20231001"); demo.createCell(1).setCellValue("示例学生");
            demo.createCell(2).setCellValue("男"); demo.createCell(3).setCellValue("信息与电子工程学院");
            demo.createCell(4).setCellValue("人工智能"); demo.createCell(5).setCellValue("大二");
            demo.createCell(6).setCellValue("AI2301"); demo.createCell(7).setCellValue("D1-101");
            demo.createCell(8).setCellValue(520); demo.createCell(9).setCellValue(0);
            demo.createCell(10).setCellValue(85.5); demo.createCell(11).setCellValue("PASS");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                    + URLEncoder.encode("学生名单导入模板.xlsx", StandardCharsets.UTF_8));
            wb.write(response.getOutputStream());
        }
    }

    public void generateGradeTemplate(HttpServletResponse response) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("课程成绩");
            String[] headers = {"学号*", "课程名称*", "学分*", "成绩*"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            Row demo = sheet.createRow(1);
            demo.createCell(0).setCellValue("20231001"); demo.createCell(1).setCellValue("高等数学");
            demo.createCell(2).setCellValue(4.0); demo.createCell(3).setCellValue(92);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                    + URLEncoder.encode("课程成绩导入模板.xlsx", StandardCharsets.UTF_8));
            wb.write(response.getOutputStream());
        }
    }

    // ===== 导入学生 =====
    public Map<String, Object> importStudents(MultipartFile file) {
        int success = 0, skip = 0, error = 0;
        List<String> errors = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String studentNo = cellStr(row.getCell(0));
                    String name = cellStr(row.getCell(1));
                    if (studentNo.isEmpty() || name.isEmpty()) { skip++; continue; }

                    String gender = cellStr(row.getCell(2));
                    String college = cellStr(row.getCell(3));
                    String major = cellStr(row.getCell(4));
                    String grade = cellStr(row.getCell(5));
                    String className = cellStr(row.getCell(6));
                    String dormNo = cellStr(row.getCell(7));
                    String cet4 = cellStr(row.getCell(8));
                    String cet6 = cellStr(row.getCell(9));
                    String peScore = cellStr(row.getCell(10));
                    String labor = cellStr(row.getCell(11));

                    if (college.isEmpty()) college = "信息与电子工程学院";
                    if (major.isEmpty()) major = "人工智能";
                    if (grade.isEmpty()) grade = "大一";

                    // User
                    User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getAccount, studentNo));
                    if (user == null) {
                        user = new User();
                        user.setAccount(studentNo); user.setName(name);
                        user.setRole("STUDENT"); user.setPasswordHash(passwordEncoder.encode(studentNo));
                        user.setStatus("ACTIVE"); user.setCreatedAt(LocalDateTime.now());
                        userMapper.insert(user);
                    } else { user.setName(name); userMapper.updateById(user); }

                    // Student
                    Student stu = studentMapper.selectOne(Wrappers.<Student>lambdaQuery().eq(Student::getStudentNo, studentNo));
                    if (stu == null) stu = new Student();
                    stu.setUserId(user.getId()); stu.setStudentNo(studentNo); stu.setName(name);
                    stu.setGender(gender.isEmpty() ? "男" : gender);
                    stu.setCollege(college); stu.setMajor(major); stu.setGrade(grade);
                    stu.setClassName(className); stu.setDormNo(dormNo);
                    if (!cet4.isEmpty()) stu.setCet4Score(Integer.parseInt(cet4));
                    if (!cet6.isEmpty()) stu.setCet6Score(Integer.parseInt(cet6));
                    if (!peScore.isEmpty()) stu.setPeScore(new BigDecimal(peScore));
                    if (!labor.isEmpty()) stu.setLaborEvaluation(labor);

                    if (stu.getId() == null) studentMapper.insert(stu);
                    else studentMapper.updateById(stu);
                    success++;
                } catch (Exception e) {
                    error++; errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
        } catch (Exception e) { throw new RuntimeException("文件解析失败: " + e.getMessage()); }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success); result.put("skip", skip);
        result.put("error", error); result.put("errors", errors);
        return result;
    }

    // ===== 导入成绩 =====
    public Map<String, Object> importGrades(MultipartFile file, Long yearId) {
        int success = 0, skip = 0, error = 0;
        List<String> errors = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String studentNo = cellStr(row.getCell(0));
                    String courseName = cellStr(row.getCell(1));
                    String creditStr = cellStr(row.getCell(2));
                    String scoreStr = cellStr(row.getCell(3));
                    if (studentNo.isEmpty() || courseName.isEmpty()) { skip++; continue; }

                    Student stu = studentMapper.selectOne(Wrappers.<Student>lambdaQuery().eq(Student::getStudentNo, studentNo));
                    if (stu == null) { error++; errors.add("第" + (i+1) + "行: 学号不存在"); continue; }

                    BigDecimal credit = creditStr.isEmpty() ? BigDecimal.ONE : new BigDecimal(creditStr);
                    BigDecimal score = scoreStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(scoreStr);

                    CourseGrade cg = courseGradeMapper.selectOne(Wrappers.<CourseGrade>lambdaQuery()
                            .eq(CourseGrade::getStudentId, stu.getId())
                            .eq(CourseGrade::getAcademicYearId, yearId)
                            .eq(CourseGrade::getCourseName, courseName));
                    if (cg == null) {
                        cg = new CourseGrade();
                        cg.setStudentId(stu.getId()); cg.setAcademicYearId(yearId);
                        cg.setCourseName(courseName); cg.setCredit(credit); cg.setScore(score);
                        courseGradeMapper.insert(cg);
                    } else { cg.setCredit(credit); cg.setScore(score); courseGradeMapper.updateById(cg); }
                    success++;
                } catch (Exception e) {
                    error++; errors.add("第" + (i+1) + "行: " + e.getMessage());
                }
            }
        } catch (Exception e) { throw new RuntimeException("文件解析失败: " + e.getMessage()); }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success); result.put("skip", skip);
        result.put("error", error); result.put("errors", errors);
        return result;
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            default -> "";
        };
    }
}
