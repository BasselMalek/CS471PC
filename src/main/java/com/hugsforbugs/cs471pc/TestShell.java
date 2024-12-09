package com.hugsforbugs.cs471pc;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.*;

public class TestShell {
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, URISyntaxException {
        ThreadPoolExecutor runner = new ThreadPoolExecutor(8, 16, 10000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
//       System.out.println(runner.submit(new FileDownloader("./test-files/test.txt", "./downloads/text.txt")));
       System.out.println(runner.submit(new FileDownloader("http://127.0.0.1/test-files/testimg-1.jpg", "./downloads/")));
//       System.out.println(runner.submit(new FileDownloader("http://127.0.0.1/test-files/lo.e", "./downloads/")));
//       System.out.println(runner.submit(new FileDownloader("http://127.0.0.1/test-files/testimg-1.jpg", "./downloads/")));
//       System.out.println(runner.submit(new FileDownloader("http://127.0.0.1/test-files/testimg-1.jpg", "./downloads/")));
//       System.out.println(runner.submit(new FileDownloader("./test-files/testpdf.pdf", "./downloads/testpdf.pdf")));
        runner.shutdown();
        runner.awaitTermination(1, TimeUnit.MINUTES);
//        d.startDownload("./test-files/test.txt", "./downloads/text.txt");
//        d.startDownload("./test-files/testimg-1.jpg", "./downloads/img1.jpg");
//        d.startDownload("./test-files/testimg-2.png", "./downloads/img2.png");
//        d.startDownload("./test-files/testpdf.pdf", "./downloads/pdf.pdf");
    }
}
