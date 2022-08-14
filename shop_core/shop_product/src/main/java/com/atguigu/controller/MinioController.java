package com.atguigu.controller;


import com.atguigu.result.RetVal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 品牌表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-22
 */
@RestController
@RequestMapping("/product/brand")
public class MinioController {
    @PostMapping("minioUpload1")
    public RetVal minioUpload1(@RequestPart("avatar") MultipartFile avatar,
                              @RequestPart("life") MultipartFile life,
                              @RequestPart("secret") MultipartFile secret){
        System.out.println("xxx");
        return RetVal.ok();
    }

    @PostMapping("minioUpload2")
    public RetVal minioUpload2(@RequestPart("avatar") MultipartFile avatar,
                              @RequestPart("life") MultipartFile[] life){
        System.out.println("xxx");
        return RetVal.ok();
    }

    @PostMapping("minioUpload3")
    public RetVal minioUpload3(@RequestPart("avatar") MultipartFile avatar,
                              @RequestPart("life") MultipartFile life,
                              @RequestParam("secretDesc") String secretDesc){
        System.out.println("xxx");
        return RetVal.ok();
    }

    @PostMapping("minioUpload")
    public RetVal minioUpload(@RequestPart("images") MultipartFile[] images){
        System.out.println("xxx");
        return RetVal.ok();
    }
}

