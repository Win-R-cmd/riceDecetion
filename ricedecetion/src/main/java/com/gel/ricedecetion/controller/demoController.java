package com.gel.ricedecetion.controller;

import com.gel.ricedecetion.pojo.RespBean;
import org.opencv.core.Mat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.Core.merge;
import static org.opencv.core.Core.split;
import static org.opencv.highgui.HighGui.imshow;
import static org.opencv.highgui.HighGui.waitKey;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.equalizeHist;

@RestController
@RequestMapping("/demo")
public class demoController {
    @GetMapping("/hello")
    public RespBean hello() {
        return RespBean.success("hello world");
    }
    @GetMapping("/hello2")
    public void hello2() throws Exception {
        System.out.println(123);
//        System.load("G:\\ideaprojects\\ricedecetion\\src\\main\\resources\\opencv_java3410.dll");
        URL url = ClassLoader.getSystemResource("lib/opencv_java3410.dll");
        System.load(url.getPath());
        Mat image = imread("G:\\010223.jpg", 1);
        if (image.empty()){
            throw new Exception("image is empty!");
        }
        imshow("Original Image", image);
        List<Mat> imageRGB = new ArrayList<>();
        split(image, imageRGB);
        for (int i = 0; i < 3; i++) {
            equalizeHist(imageRGB.get(i), imageRGB.get(i));
        }
        merge(imageRGB, image);
        imshow("Processed Image", image);
        System.setProperty("java.awt.headless", "false");
        waitKey();
    }
}
