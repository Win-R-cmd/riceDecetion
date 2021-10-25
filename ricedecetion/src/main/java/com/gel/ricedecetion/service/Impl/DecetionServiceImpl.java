package com.gel.ricedecetion.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gel.ricedecetion.config.WebConfig;
import com.gel.ricedecetion.mapper.DecetionMapper;
import com.gel.ricedecetion.pojo.Decetion;
import com.gel.ricedecetion.pojo.SeedData;
import com.gel.ricedecetion.service.IDecetionService;
import com.gel.ricedecetion.util.ImgPathUtils;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.ls.LSOutput;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

@Service
public class DecetionServiceImpl extends ServiceImpl<DecetionMapper, Decetion> implements IDecetionService {
    @Autowired
    private DecetionMapper decetionMapper;

    @Override
    public List<String> riceDecetion(Mat srcImage) throws Exception {
//        System.out.println("调用");
//        // 调用opencv的jar包
//        URL url = ClassLoader.getSystemResource("lib/opencv_java3410.dll");
//        System.load(url.getPath());

        if (srcImage.empty()) {
            throw new Exception("image is empty!");
        }
        Mat grayImage = srcImage.clone(),
                dstImage = srcImage.clone(),
                vagueImage = srcImage.clone(),
                adathrImage = srcImage.clone(),
                kaiImage = srcImage.clone();

        Imgproc.cvtColor(srcImage, grayImage, Imgproc.COLOR_BGR2GRAY); // 灰度化图像
        // 保存图片
        Imgcodecs.imwrite(ImgPathUtils.uploadImg(), grayImage);
        Imgproc.medianBlur(grayImage, vagueImage, 7); // 图像模糊
        Imgcodecs.imwrite(ImgPathUtils.uploadImg(), vagueImage);

        Imgproc.adaptiveThreshold(vagueImage, adathrImage, 255,  // 二值化
                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 101, -20);
        Imgcodecs.imwrite(ImgPathUtils.uploadImg(), dstImage);

        List<MatOfPoint> contours = new ArrayList<>();
        MatOfPoint con;
        Mat hierarchy = srcImage.clone();

        Mat kernel = new Mat(new Size(2, 2), CvType.CV_8U);
        Imgproc.morphologyEx(adathrImage, kaiImage, Imgproc.MORPH_OPEN, kernel);
        Imgproc.findContours(kaiImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, // 轮廓化
                Imgproc.CHAIN_APPROX_SIMPLE);
        dstImage = kaiImage;
        Imgcodecs.imwrite(ImgPathUtils.uploadImg(), kaiImage);

        ListIterator<MatOfPoint> contour = contours.listIterator();
        int cnt = 0, uncnt = 0; // cnt为正常种子的个数，uncnt为异常种子的个数
        SeedData[] seed = new SeedData[1000], // 用来存储正常种子的长宽
                unseed;    // 用来储存异常种子的长宽
        SeedData seedtemp = new SeedData(); // 用来临时储存种子长宽
        double avg_area = 0, sum_area = 0;

        MatOfPoint2f Newmop;
        RotatedRect roet;
        Rect bret;
        Point[] point = new Point[4];

        while (contour.hasNext()) {
            con = contour.next();
            double area = Imgproc.contourArea(con); // 获取当前扫描到的面积
            if (area > 700) {

                Newmop = new MatOfPoint2f(con.toArray());
                roet = Imgproc.minAreaRect(Newmop);// 获取轮廓的最小外接矩形
                seedtemp.width = roet.size.width;
                seedtemp.height = roet.size.height;
                if (seedtemp.width > seedtemp.height) {
                    double t = seedtemp.width;
                    seedtemp.width = seedtemp.height;
                    seedtemp.height = t;
                }
                bret = Imgproc.boundingRect(Newmop);// 获取外接矩形的左上角顶点
                roet.points(point);// 获取最小外接矩形的四个顶点

                if (isAbnormal(area, avg_area) && cnt != 0) {
                    uncnt++;
                    MatOfInt hull = new MatOfInt();
                    MatOfInt4 defects = new MatOfInt4();
                    Imgproc.convexHull(con, hull, false);
                    if (hull.toArray().length > 7) {
                        // 缺陷计算
                        Imgproc.convexityDefects(con, hull, defects);

                        if (defects.toArray().length > 2) {
                            int flagmax = -1, flagtwo = -1;
                            {//用来求最大
                                for (int k = 3; k >= 0; k--) {
                                    // 求最大的那一组
                                    double maxtemp = defects.get(0, 0)[k];
                                    int max = 0;
                                    for (int i = 1; i < defects.rows(); i++) {
                                        if (maxtemp < defects.get(i, 0)[k]) {
                                            maxtemp = defects.get(i, 0)[k];
                                            max = i;
                                        } else if (maxtemp == defects.get(i, 0)[k]) {
                                            if (defects.get(max, 0)[k - 1] < defects.get(i, 0)[k - 1]) {
                                                max = i;
                                            }
                                        }
                                    }

                                    // 求第二大的那一组
                                    double twotemp = defects.get(0, 0)[k];
                                    int two = 0;
                                    for (int i = 0; i < defects.rows(); i++) {
                                        if (i != max) {
                                            if (twotemp < defects.get(i, 0)[k]) {
                                                twotemp = defects.get(i, 0)[k];
                                                two = i;
                                            } else if (twotemp == defects.get(i, 0)[k]) {
                                                if (defects.get(two, 0)[k - 1] < defects.get(i, 0)[k - 1]) {
                                                    two = i;
                                                }
                                            }
                                        }
                                    }

                                    flagmax = max;
                                    flagtwo = two;
                                    if (flagmax != -1 && flagtwo != -1)
                                        break;
                                }
                            }// 求两组最远的点结束
                            double[] max_point = defects.get(flagmax, 0),//max_point[0~3]相当于os,oe,of,od
                                    two_point = defects.get(flagtwo, 0);
                            double[] ostart = con.get((int) max_point[2], 0),
                                    tstart = con.get((int) two_point[2], 0);
                            if (max_point[3] / 256 > 15.0) //如果在像素级大于10倍则需要划分
                                Imgproc.line(kaiImage, new Point(ostart[0], ostart[1]),
                                        new Point(tstart[0], tstart[1]), new Scalar(0, 0, 255), 2);
                        }
                    }// 缺陷计算
                } else {
                    cnt++;
                    seed[cnt] = new SeedData();
                    seed[cnt].width = seedtemp.width;
                    seed[cnt].height = seedtemp.height;
                    sum_area += area;
                    avg_area = sum_area / cnt;
                }

            }//while

        }

        List<MatOfPoint> dsts = new ArrayList<>();

        Imgproc.findContours(dstImage, dsts, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        ListIterator<MatOfPoint> list = dsts.listIterator();
        MatOfPoint mop;
        Newmop = new MatOfPoint2f();
        roet = new RotatedRect();
        bret = new Rect();
        point = new Point[4];
        cnt = 0;
        uncnt = 0; // cnt为正常种子的个数，uncnt为异常种子的个数
        seed = new SeedData[1000]; // 用来存储正常种子的长宽
        unseed = new SeedData[100];    // 用来储存异常种子的长宽
        seedtemp = new SeedData(); // 用来临时储存种子长宽
        avg_area = 0;
        sum_area = 0;
        double ruler_len = 44.5, ruler_pixel = 0, ruler;
        while (list.hasNext()) {
            mop = list.next();
            double area = Imgproc.contourArea(mop); // 获取当前扫描到的面积
            if (area >= 700) {

                Newmop = new MatOfPoint2f(mop.toArray());
                roet = Imgproc.minAreaRect(Newmop);// 获取轮廓的最小外接矩形
                seedtemp.width = roet.size.width;
                seedtemp.height = roet.size.height;
                if (seedtemp.width > seedtemp.height) {
                    double t = seedtemp.width;
                    seedtemp.width = seedtemp.height;
                    seedtemp.height = t;
                }
                bret = Imgproc.boundingRect(Newmop);// 获取外接矩形的左上角顶点
                roet.points(point);// 获取最小外接矩形的四个顶点
//	    				Imgproc.rectangle(srcImage, new Point(bret.x,bret.y),
//	    						new Point(bret.x+bret.width,bret.y+bret.height),
//	    						 new Scalar(0,0,255) );// 绘制矩形
                if (isAbnormal(area, avg_area) && cnt != 0) {
                    uncnt++;
                    for (int j = 0; j < 4; j++) { // 绘制四条矩形边界
                        Imgproc.line(srcImage, point[j], point[(j + 1) % 4], new Scalar(0, 255, 0), 2, 8);  //绘制最小外接矩形每条边
                    }
                    Imgproc.putText(srcImage, String.valueOf(uncnt), new Point(bret.x, bret.y), Core.FONT_HERSHEY_SIMPLEX
                            , 0.8, new Scalar(255, 255, 0), 1); // 添加序号
                    unseed[uncnt] = new SeedData();
                    unseed[uncnt].width = seedtemp.width;
                    unseed[uncnt].height = seedtemp.height;
                    int t = (int) (area / avg_area);
                    uncnt += t;
                    if (area - t * avg_area <= 0.5 * avg_area) uncnt--;
                    if ((ruler = Math.max(seedtemp.width, seedtemp.height)) > ruler_pixel)
                        ruler_pixel = ruler;
                } else {
                    cnt++;
                    for (int j = 0; j < 4; j++) { // 绘制四条矩形边界
                        Imgproc.line(srcImage, point[j], point[(j + 1) % 4], new Scalar(0, 0, 255), 2, 8);  //绘制最小外接矩形每条边
                    }
                    Imgproc.putText(srcImage, String.valueOf(cnt), new Point(bret.x, bret.y), Core.FONT_HERSHEY_SIMPLEX
                            , 0.8, new Scalar(255, 255, 255), 1); // 添加序号
                    seed[cnt] = new SeedData();
                    seed[cnt].width = seedtemp.width;
                    seed[cnt].height = seedtemp.height;
                    sum_area += area;
                    avg_area = sum_area / cnt;
                }
            }
        }    // while
        System.out.println("水稻种子的总个数为：" + (cnt + uncnt));

        String path = ImgPathUtils.uploadImg();
        String absoluteFilePath = ImgPathUtils.getAbsoluteFile(WebConfig.getBaseDir() + path).getAbsolutePath();
        Decetion decetion = new Decetion();
        decetion.setCreate_time(LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault()));
        decetion.setPath(absoluteFilePath);
        decetionMapper.insert(decetion);
//        System.out.println(path);
//        System.out.println(absoluteFilePath);
        Imgcodecs.imwrite(absoluteFilePath, srcImage);
        List<String> list1 = new ArrayList();
        list1.add(path);  // list1[0]是路径

        // 向list1中添加长宽数据
        for (int i = 1; i <= cnt; i++) {
            if (seed[i].width > seed[i].height) {
                double temp = seed[i].width;
                seed[i].width = seed[i].height;
                seed[i].height = temp;
            }
            list1.add(String.valueOf(seed[i].height/ ruler_pixel * ruler_len).substring(0,5)); // 添加长
            list1.add(String.valueOf(seed[i].width/ ruler_pixel * ruler_len).substring(0,5)); // 添加宽
        }
        return list1;
    }

    public static boolean isAbnormal(double src, double avg_area) {
        boolean flag = false;
        if (src >= 1.5 * avg_area) {
            flag = true;
        }
        return flag;
    }

    /**
     * bufferedImage convert mat
     *
     * @param im
     * @return
     */
    @Override
    public Mat convertMat(BufferedImage im) {
        // Convert INT to BYTE
        im = toBufferedImageOfType(im, BufferedImage.TYPE_3BYTE_BGR);
        // Convert bufferedimage to byte array
        byte[] pixels = ((DataBufferByte) im.getRaster().getDataBuffer())
                .getData();
        // Create a Matrix the same size of image
        Mat image = new Mat(im.getHeight(), im.getWidth(), 16);
        // Fill Matrix with image values
        image.put(0, 0, pixels);
        return image;
    }

    /**
     * 8-bit RGBA convert 8-bit RGB
     *
     * @param original
     * @param type
     * @return
     */
    private static BufferedImage toBufferedImageOfType(BufferedImage original, int type) {
        if (original == null) {
            throw new IllegalArgumentException("original == null");
        }

        // Don't convert if it already has correct type
        if (original.getType() == type) {
            return original;
        }
        // Create a buffered image
        BufferedImage image = new BufferedImage(original.getWidth(), original.getHeight(), type);
        // Draw the image onto the new buffer
        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(original, 0, 0, null);
        } finally {
            g.dispose();
        }

        return image;
    }
}
