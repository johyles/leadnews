package com.heima.minio.test;

import com.heima.file.service.FileStorageService;
import com.heima.minio.MinIOApplication;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
@SpringBootTest(classes = MinIOApplication.class)
@RunWith(SpringRunner.class)
public class MinIOTest {

    @Autowired
    private FileStorageService fileStorageService;

    //把list.html文件上传到minio中，并且可以在浏览器中访问
    @Test
    public void test() throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream("D:\\work_place\\IDEA\\leadnews\\test_html\\list.html");
        String path = fileStorageService.uploadHtmlFile("", "list.html", fileInputStream);
        System.out.println(path);
    }


    /**
     * 把list.html文件上传到minio中，并且可以在浏览器中访问
     * @param args
     */
    public static void main(String[] args) {

        try {
            FileInputStream fileInputStream = new FileInputStream("C:\\Users\\16090\\Desktop\\模板文件\\plugins\\js\\index.js");

            //1.获取minio的连接信息，创建一个minio的客户端

            MinioClient minioClient = MinioClient.builder().
                    credentials("minioadmin", "minioadmin").endpoint("http://192.168.200.130:9000").build();

            //2.上传
            PutObjectArgs putObjectArgs= PutObjectArgs.builder()
                    .object("plugins/js/index.js")//文件名
                    .contentType("text/javascript") //文件类型
                    .bucket("leadnews") //桶名称  与minio创建的名词一致
                    .stream(fileInputStream, fileInputStream.available(), -1)
                    .build();
            minioClient.putObject(putObjectArgs);

            //访问路径
            //System.out.println("http://192.168.200.130:9001/leadnews/list.html");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
