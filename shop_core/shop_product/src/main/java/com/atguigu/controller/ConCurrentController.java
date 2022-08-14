package com.atguigu.controller;


import com.atguigu.service.BaseBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 品牌表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-22
 */
@RestController
@RequestMapping("/product")
public class ConCurrentController {
    @Autowired
    private BaseBrandService brandService;

    @GetMapping("setNum")
    public String setNum() {
        brandService.setNum();
        return "success";
    }


}

