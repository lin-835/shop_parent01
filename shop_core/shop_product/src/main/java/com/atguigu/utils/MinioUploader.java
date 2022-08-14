package com.atguigu.utils;

import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@EnableConfigurationProperties(MinioProperties.class)
@Configuration
public class MinioUploader {
    @Autowired
    private MinioProperties minioProperties;
    @Autowired
    private MinioClient minioClient;

    /**
     * 启动项目的时候初始化bean
     * 把初始化minioClient的工作交给spring去做
     */
    @Bean
    public MinioClient minioClient() throws Exception {
        MinioClient minioClient = new MinioClient(minioProperties.getEndpoint(), minioProperties.getAccessKey(), minioProperties.getSecretKey());
        boolean isExist = minioClient.bucketExists(minioProperties.getBucketName());
        if(isExist) {
            System.out.println("Bucket already exists.");
        } else {
            //创建一个名为asiatrip的存储桶，用于存储照片的zip文件。
            minioClient.makeBucket(minioProperties.getBucketName());
        }
        return minioClient;
    }

    public String uploadFile(MultipartFile file) throws Exception {
        String fileName= UUID.randomUUID().toString()+file.getOriginalFilename();
        InputStream inputStream = file.getInputStream();;
        PutObjectOptions putObjectOptions = new PutObjectOptions(inputStream.available(), -1);
        putObjectOptions.setContentType(file.getContentType());
        minioClient.putObject(minioProperties.getBucketName(),fileName, inputStream,putObjectOptions);
        //返回文件上传成功之后的地址 http://192.168.18.208:9000/java0212/new.jpg
        String retUrl=minioProperties.getEndpoint()+"/"+minioProperties.getBucketName()+"/"+fileName;
        System.out.println("上传成功"+retUrl);
        return retUrl;

    }
}