import net.sf.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MailAgentThread extends WebService {
    private InputStream in = null;
    private OutputStream out = null;
    private Map<String, String> params = null;
    //用来保存对应的smtp的地址和对应的认证信息等
    private Map<String, List<String>> serverAddressInfo = null;

    //账号：2403116996
    //授权码：xevyrfcqimxfdhia

    MailAgentThread(Socket socket) {
        try {
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
            this.params = new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        boolean state = false;
        String apiInfo = readApiInfo(this.in);
        params.clear();
        if (!apiInfo.contains("?")) {
            System.out.println("[万恶之原]mailAgentService请求中未带有参数！");
            reportError();
            return;
        }
        for (String item : apiInfo.split("\\?")[1].split("&")) {
            params.put(item.split("=")[0], item.split("=")[1]);
        }
        String serverName = params.get("username").split("@")[1];
        params.put("username", params.get("username").split("@")[0]);
        MailAgent mailAgent = new MailAgent("smtp." + serverName);
        state = mailAgent.login(params.get("username"), params.get("password"));
        if (!state) {
            System.out.println("[mailAgentService]服务中账号密码登陆失败！");
            reportError();
            return;
        }
        if (!params.containsKey("type")) {
            reportError();
            return;
        }
        if (params.get("type").equals("send")) {
            state = mailAgent.sendMail(params);
            if (state) {
                String result = "HTTP/1.1 200 ok \n" +
                        "Content-Type: text/html \n" +
                        "Access-Control-Allow-Origin: *\n";
                try {
                    this.out.write(result.getBytes());
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.get("type").equals("query")) {
            //todo:这里是查询邮件的业务逻辑
            String mailJson = mailAgent.queryMail(params);
            String result = "HTTP/1.1 200 ok \n" +
                    "Content-Type: application/json;charset=UTF-8 \n" +
                    "Access-Control-Allow-Origin: *\n" +
                    "\n" + mailJson;
            try {
                out.write(result.getBytes());
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if (params.get("type").equals("delete")){
            JSONObject json = new JSONObject();
            state = mailAgent.deleteMail(params);
            json.put("deleteState",state);
            String result = "HTTP/1.1 200 ok \n" +
                    "Content-Type: application/json;charset=UTF-8 \n" +
                    "Access-Control-Allow-Origin: *\n" +
                    "\n" + json.toString();
            try {
                out.write(result.getBytes());
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void reportError() {
        String result = "HTTP/1.1 400 ok \n" +
                "Content-Type: text/html \n" +
                "Access-Control-Allow-Origin: *\n";
        try {
            this.out.write(result.getBytes());
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
