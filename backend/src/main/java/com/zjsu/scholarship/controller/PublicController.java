package com.zjsu.scholarship.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zjsu.scholarship.common.R;
import com.zjsu.scholarship.entity.*;
import com.zjsu.scholarship.mapper.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final ApplicationMapper applicationMapper;
    private final ScholarshipProjectMapper projectMapper;
    private final ScholarshipLevelMapper levelMapper;
    private final StudentMapper studentMapper;

    public PublicController(ApplicationMapper applicationMapper, ScholarshipProjectMapper projectMapper,
                            ScholarshipLevelMapper levelMapper, StudentMapper studentMapper) {
        this.applicationMapper = applicationMapper;
        this.projectMapper = projectMapper;
        this.levelMapper = levelMapper;
        this.studentMapper = studentMapper;
    }

    @GetMapping("/results")
    public R<List<Map<String, Object>>> results(@RequestParam(required = false) String keyword) {
        List<Application> apps = applicationMapper.selectList(Wrappers.<Application>lambdaQuery()
                .in(Application::getStatus, Arrays.asList("APPROVED", "PUBLISHED")));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Application app : apps) {
            Student s = studentMapper.selectById(app.getStudentId());
            ScholarshipProject p = projectMapper.selectById(app.getProjectId());
            ScholarshipLevel level = app.getFinalLevelId() == null ? null : levelMapper.selectById(app.getFinalLevelId());
            if (s == null || p == null) continue;
            if (keyword != null && !keyword.isEmpty()) {
                String k = keyword.toLowerCase();
                if (!s.getStudentNo().toLowerCase().contains(k)
                        && !s.getName().toLowerCase().contains(k)
                        && !p.getProjectName().toLowerCase().contains(k)) continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("studentNo", s.getStudentNo());
            m.put("name", s.getName());
            m.put("college", s.getCollege());
            m.put("major", s.getMajor());
            m.put("projectName", p.getProjectName());
            m.put("levelName", level == null ? null : level.getLevelName());
            m.put("amount", level == null ? null : level.getAmount());
            m.put("basicTotal", app.getSnapshotBasicTotal());
            m.put("abilityTotal", app.getSnapshotAbilityTotal());
            m.put("basicRank", app.getSnapshotBasicRank());
            m.put("abilityRank", app.getSnapshotAbilityRank());
            list.add(m);
        }
        return R.ok(list);
    }

    @GetMapping("/projects")
    public R<List<Map<String, Object>>> publishedProjects() {
        List<ScholarshipProject> ps = projectMapper.selectList(Wrappers.<ScholarshipProject>lambdaQuery()
                .in(ScholarshipProject::getStatus, Arrays.asList("PUBLISHED", "REVIEWING", "OPEN"))
                .orderByDesc(ScholarshipProject::getId));
        List<Map<String, Object>> out = new ArrayList<>();
        for (ScholarshipProject p : ps) {
            Map<String, Object> m = new HashMap<>();
            m.put("project", p);
            m.put("levels", levelMapper.selectList(Wrappers.<ScholarshipLevel>lambdaQuery()
                    .eq(ScholarshipLevel::getProjectId, p.getId())
                    .orderByAsc(ScholarshipLevel::getLevelOrder)));
            out.add(m);
        }
        return R.ok(out);
    }
}
