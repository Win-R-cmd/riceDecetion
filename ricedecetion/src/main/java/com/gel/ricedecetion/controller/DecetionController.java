package com.gel.ricedecetion.controller;

import com.gel.ricedecetion.config.WebConfig;
import com.gel.ricedecetion.pojo.RespBean;
import com.gel.ricedecetion.service.IDecetionService;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping(value = "img")
public class DecetionController {
    @Autowired
    private IDecetionService IDecetionService;

    /**
     * 上传图片
     *
     * @param request
     */
    @RequestMapping("/uploadImg")
    public RespBean uploadImg(HttpServletRequest request) {
        try {
            StandardMultipartHttpServletRequest req = (StandardMultipartHttpServletRequest) request;
            Iterator<String> iterator = req.getFileNames();
            List<List<String>> arr = new ArrayList<>();
            while (iterator.hasNext()) {
                MultipartFile file = req.getFile(iterator.next());
                if (!file.isEmpty()) {
                    // 调用opencv的jar包
                    URL url = ClassLoader.getSystemResource("lib/opencv_java3410.dll");
                    System.load(url.getPath());

                    // 转BufferedImage对象
                    BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
                    // bufferedImage转Mat
                    Mat mat = IDecetionService.convertMat(bufferedImage);
                    // 水稻检测，返回路径
                    arr.add(IDecetionService.riceDecetion(mat));
                }
            }
            for (int i =0;i<arr.size();i++){
                System.out.println(arr.get(i));
            }
            return RespBean.success("上传成功", arr);
        } catch (Exception e) {
            return RespBean.error("上传失败");
        }
    }

    /**
     * 下载图片
     *
     * @param url      请求的图片url
     * @param response
     */
    @GetMapping("/downloadImg")
    public void downloadFile(String url, HttpServletResponse response) {
//        url = url.substring(2,url.length()-2);
        System.out.println(url);
        File file = new File(File.separator + WebConfig.getBaseDir() + url);

        if (!(file.exists() && file.canRead())) {
            System.out.println("文件不存在");
        }
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            inputStream.read(data, 0, data.length);
            inputStream.close();
            response.setContentType("image/png;charset=utf-8");
            OutputStream stream = response.getOutputStream();
            stream.write(data);
            stream.flush();
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
