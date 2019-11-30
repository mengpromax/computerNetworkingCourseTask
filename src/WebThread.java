import java.io.*;
import java.net.Socket;

public class WebThread extends Thread {
    private static String ROOT = "";
    private InputStream inputStream;
    private OutputStream outputStream;

    WebThread(Socket sk) {
        String defaultDir = ReadConfigUtils.configList.getOrDefault("defaultWebDir","/webPage");
        ROOT = System.getProperty("user.dir") + defaultDir;
        try {
            this.inputStream = sk.getInputStream();
            this.outputStream = sk.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String filePath = read();
        response(filePath);
    }

    private void response(String filePath) {
        if(filePath.equals("/")){
            filePath = ReadConfigUtils.configList.getOrDefault("defaultPage", "/index.html");
        }
        try {
            File file = new File(ROOT + filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String readLine = null;
            StringBuilder sb = new StringBuilder();
            while ((readLine = reader.readLine()) != null) {
                sb.append(readLine).append("\r\n");
            }
            String result = "HTTP/1.1 200 ok \n" +
                    "Content-Type: text/html \n" +
                    "\n" + sb.toString();
            this.outputStream.write(result.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            response("/error.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String read() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(this.inputStream));
        String readLine = null;
        String[] split = null;
        try {
            readLine = reader.readLine();
            split = readLine.split(" ");
            if (split.length != 3)
                return null;
            System.out.println(String.format("[万恶之原]-web服务器收到的http报文首部：%s!",readLine));
            return split[1];
        } catch (Exception e) {
            //todo:在浏览器访问的时候，会出现很多不需要的访问，所以在这里会出现相关的错误信息
        }
        return null;
    }
}
