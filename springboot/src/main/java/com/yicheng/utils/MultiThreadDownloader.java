package com.yicheng.utils;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class MultiThreadDownloader  {
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36";


    public void download(String url,String localPath,int threadCount) throws Exception {
        long fileSize=getFileSize(url);
        System.out.println("要下载的文件总大小为"+fileSize+"字节");

        if(fileSize<=0){
            System.out.println("文件无法访问 退出");
            return;
        }

        // 在本地先开辟一个与要下载文件一样大的空文件
        RandomAccessFile raf=new RandomAccessFile(localPath,"rw");
        raf.setLength(fileSize);
        raf.close();

        //准备线程池和倒计时锁
        ExecutorService executor= Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch=new CountDownLatch(threadCount);

        long blockSize=fileSize/threadCount;
        long startPos,endPos;
        for(int i=0;i<threadCount;i++){
            startPos=i*blockSize;

            //如果是最后一个线程 直接把剩下的全部给他 防止除不尽或者漏掉最后几个字节
            if(i==threadCount-1){
                endPos=fileSize-1;
            }else{
                endPos=(i+1)*blockSize-1;
            }

            DownloadTask worker=new DownloadTask(url,localPath,startPos,endPos,i,latch);
            executor.execute(worker);
        }

        //主线程阻塞等待
        System.out.println("所有线程已经开始工作...主线程阻塞等待... ");
        latch.await();
        System.out.println("主线程：文件已经下载成功 下载至"+localPath);

        executor.shutdown();//关闭线程池

    }

    private long getFileSize(String urlStr) throws Exception {
        URL url=new URL(urlStr);
        HttpURLConnection conn=(HttpURLConnection)url.openConnection();//创建连接
        conn.setConnectTimeout(5000);
        conn.setRequestMethod("HEAD");
        //加上这一行，伪装成 Chrome 浏览器
        conn.setRequestProperty("User-Agent", UA);
        int code=conn.getResponseCode();
        if(code==200){
            return conn.getContentLengthLong(); //200代表成功连接
        }
        return -1;
    }


    class DownloadTask implements Runnable{

        private String url;//文件下载地址
        private String localPath;//本地保存路径
        private long startPos;
        private long endPos;
        private int workId;
        private CountDownLatch latch;

        DownloadTask(String url, String localPath, long startPos, long endPos,int workId,CountDownLatch latch) {
            this.url = url;
            this.localPath = localPath;
            this.startPos = startPos;
            this.endPos = endPos;
            this.workId = workId;
            this.latch = latch;
        }


        @Override
        public void run() {
            //在这边创建流  方便关闭
            HttpURLConnection connection = null;//网络连接类
            RandomAccessFile raf = null;//写文件
            InputStream inputStream = null; //读文件

            try {


                URL downloadUrl = new URL(this.url);//用把字符串网址变成URL对象
                connection=(HttpURLConnection) downloadUrl.openConnection();//打开URL的连接

                connection.setRequestMethod("GET");//设置请求方式： get下载 head获取文件信息 不要内容
                connection.setConnectTimeout(5000);//设置网络超时链接的时间

                connection.setRequestProperty("User-Agent", UA);

                //设置Http 请求头 Range 格式 bytes=0-1024
                connection.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);

                //准备本地文件写入
                raf = new RandomAccessFile(this.localPath, "rw"); //模式设置为读写

                //把指针移动到对应的位置 否则会从头覆盖写
                raf.seek(startPos);

                //获取网络连接的响应码
                int responseCode = connection.getResponseCode();
                //206代表部分内容 代表range成功
                if (responseCode == 206) {
                    //得到网络连接的输入流
                    inputStream = connection.getInputStream();

                    //缓冲区 8Kb 用来一次读取8Kb的内容
                    byte[] buffer = new byte[1024*8];

                    int len;

                    while((len=inputStream.read(buffer))!=-1){
                        raf.write(buffer,0,len);

                    }


                    //输出信息
                    System.out.println("工人"+workId+"号搞定 下载范围为"+startPos+"-"+endPos);

                }else{
                    System.out.println("工人"+workId+"号 服务器不支持分块下载 或者请求失败 响应码为: "+responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                //关闭各种流 判断非空
                try{
                    if(connection!=null){connection.disconnect();}
                    if(inputStream!=null){inputStream.close();}
                    if(raf!=null){raf.close();}
                }catch(IOException e){
                    e.printStackTrace();
                }

                latch.countDown();//计数器减1

            }
        }
    }
}
