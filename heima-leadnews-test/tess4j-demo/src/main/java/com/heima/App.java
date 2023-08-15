package com.heima;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;

/**
 * Hello world!
 *
 */
public class App
{
    /**
     * 识别图片文字
     * @param args
     */
    public static void main( String[] args ) throws TesseractException {

        //创建实例
        ITesseract tesseract = new Tesseract();

        //设置字体库路径
        tesseract.setDatapath("D:\\work_place\\IDEA\\hm_leadnews\\day04");

        //设置语言
        tesseract.setLanguage("chi_sim");

        File file = new File("D:\\work_place\\IDEA\\hm_leadnews\\day04\\image-20210524161243572.png");
        //识别图片
        String result = tesseract.doOCR(file);

        System.out.println("识别结果是"+result);

    }
}
