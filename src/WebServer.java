import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WebServer {
    private String rootDir = "";
    private String defaultPage = "";
    private static WebService service = null;
    private static List<String> classList = null;
    private static ServerSocket socket = null;

    public static void main(String[] args) throws IOException {
        ReadConfigUtils.getConfigList(null);
        System.out.println("[万恶之原]-web服务器正在启动！");
        System.out.println("[万恶之原]-web服务器正在读取配置文件！");
        classList = getFiles(System.getProperty("user.dir") + "/WebPage");
        if(ReadConfigUtils.configList.getOrDefault("isUseLogging","false").equals("true")){
            System.out.println("[万恶之原]-web服务器正在配置日志文件！");
            System.out.println("[万恶之原]-web服务器日志文件配置完成，所有输出都将保存到日志文件中！");
            File f = new File(ReadConfigUtils.configList.getOrDefault("loggingFile","logging.txt"));
            if (!f.exists()) {
                f.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(f);
            PrintStream printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
        }
        for (String item : classList) {
            if (!item.contains("Service")) {
                continue;
            }
            try {
                Class tempClass = Class.forName(item);
                service = (WebService) tempClass.newInstance();
                System.out.println("[万恶之原]-服务----" + service.getClass().getName() + "----启动完毕！");
                service.start();
            } catch (Exception e) {
                System.out.println("[万恶之原]-加载----" + item + "----启动器时出现问题!");
            }
        }
        System.out.println("[万恶之原]-Web服务器已经启动完毕!");
        int port = Integer.parseInt(ReadConfigUtils.configList.get("port"));
        socket = new ServerSocket(port);
        while (true) {
            Socket sk = socket.accept();
            System.out.println(String.format("[万恶之原]-web服务器接收到来自IP:%s的请求!", sk.getInetAddress()));
            new WebThread(sk).start();
        }
    }

    private static List<String> getFiles(String path) {
        List<String> list = new ArrayList<>();
        File file = new File(path);
        String name = null;
        File[] files = file.listFiles();
        for (int i = 0; i < Objects.requireNonNull(files).length; i++) {
            name = files[i].getPath().split("\\\\")[files[i].getPath().split("\\\\").length - 1];
            if (name.split("[.]")[name.split("[.]").length - 1].equals("java")) {
                list.add(name.split("[.]")[0]);
            }
        }
        return list;
    }
}
