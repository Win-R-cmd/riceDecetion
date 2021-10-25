package com.gel.ricedecetion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gel.ricedecetion.pojo.Decetion;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;
import java.util.List;

public interface IDecetionService extends IService<Decetion> {
    List<String> riceDecetion(Mat srcImage) throws Exception;

    Mat convertMat(BufferedImage im);
}
