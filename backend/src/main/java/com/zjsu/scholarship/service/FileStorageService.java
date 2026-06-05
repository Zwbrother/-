package com.zjsu.scholarship.service;

import com.zjsu.scholarship.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("文件为空");
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String subDir = LocalDate.now().toString();
        // 使用绝对路径，避免在 JAR 运行时解析到 Tomcat 临时目录
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dir = baseDir.resolve(subDir);
        try {
            Files.createDirectories(dir);
            Path dest = dir.resolve(fileName);
            try (var in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + subDir + "/" + fileName;
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }
    }
}
