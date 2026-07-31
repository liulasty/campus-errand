package com.lz.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lz.pojo.result.Result;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/*
 * Created with IntelliJ IDEA.
 * @Author: lz
 * @Date: 2024/04/14/23:36
 * @Description: 图片上传/删除（七牛云对象存储）
 */

/**
 * @author lz
 */
@RestController
@RequestMapping("/img")
@Slf4j
@Api(tags = "上传图片")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE })
public class ImgController {

    private static final List<String> ALLOWED_EXT = Arrays.asList(".jpg", ".jpeg", ".png", ".gif");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    @Value("${qiniu.accessKey}")
    private String accessKey;

    @Value("${qiniu.secretKey}")
    private String secretKey;

    @Value("${qiniu.bucket}")
    private String bucket;

    @Value("${qiniu.domain}")
    private String domain;

    /**
     * 通用图片上传
     *
     * @param file 文件
     *
     * @return {@code Result<String>} 返回访问路径（域名/目录/key）
     */
    @PostMapping("/upload")
    @ApiOperation("通用图片上传")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return upload(file, "photos");
    }

    /**
     * 头像上传
     *
     * @param file 文件
     *
     * @return {@code Result<String>} 返回访问路径（域名/目录/key）
     */
    @PostMapping("/uploadAvatar")
    @ApiOperation("头像上传")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return upload(file, "avatar");
    }

    /**
     * 删除图片
     *
     * @param filePath        文件 key 或 域名/文件 key（query 参数）
     * @param deleteImagesUrl 兼容前端 ossUrl 字段
     *
     * @return {@code Result<String>}
     */
    @DeleteMapping("/delete")
    @ApiOperation("删除图片")
    public Result<String> deleteImage(
            @RequestParam(value = "filePath", required = false) String filePath,
            @RequestParam(value = "deleteImagesUrl", required = false) String deleteImagesUrl) {
        String key = filePath != null ? filePath : deleteImagesUrl;
        if (key == null || key.trim().isEmpty()) {
            return Result.error("请提供要删除的文件");
        }
        key = extractKey(key);
        try {
            Auth auth = Auth.create(accessKey, secretKey);
            Configuration cfg = new Configuration(Region.region0());
            BucketManager bucketManager = new BucketManager(auth, cfg);
            bucketManager.delete(bucket, key);
            return Result.success("删除成功");
        } catch (QiniuException e) {
            log.error("删除图片失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    private Result<String> upload(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.error("仅支持 jpg/jpeg/png/gif 图片");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.error("图片大小不能超过 5MB");
        }
        String key = folder + "/" + UUID.randomUUID() + ext;
        try {
            Auth auth = Auth.create(accessKey, secretKey);
            String upToken = auth.uploadToken(bucket);
            Configuration cfg = new Configuration(Region.region0());
            UploadManager uploadManager = new UploadManager(cfg);
            Response response = uploadManager.put(file.getBytes(), key, upToken);
            if (!response.isOK()) {
                log.error("七牛上传失败: {}", response.bodyString());
                return Result.error("上传失败");
            }
            return Result.success(domain + "/" + key, "上传成功");
        } catch (QiniuException e) {
            log.error("上传图片失败", e);
            return Result.error("上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("上传图片异常", e);
            return Result.error("上传失败");
        }
    }

    /**
     * 从「域名/key」或「http(s)://域名/key」或纯 key 中提取对象 key
     */
    private String extractKey(String filePathOrUrl) {
        String s = filePathOrUrl.trim();
        if (s.startsWith("http://") || s.startsWith("https://")) {
            int slashIndex = s.indexOf('/', s.indexOf("://") + 3);
            return slashIndex > 0 ? s.substring(slashIndex + 1) : s;
        }
        if (s.startsWith(domain + "/")) {
            return s.substring(domain.length() + 1);
        }
        return s;
    }
}
