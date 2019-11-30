import net.sf.json.JSONObject;

import javax.mail.internet.MimeUtility;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

class MailAgent {
    private final static String CRLF = "\r\n";
    private String mailServerAddress;
    private String username = null;
    private String password = null;
    private Socket socket = null;
    private JSONObject jsonObject = null;
    private OutputStream out = null;
    private InputStream in = null;
    private BufferedReader br = null;
    private PrintWriter printWriter;
    private String response = null;
    private String postFix = null;

    MailAgent(String mailServerAddress) {
        this.mailServerAddress = mailServerAddress;
        this.postFix = this.mailServerAddress.split("mtp.")[1];
        try {
            socket = new Socket(mailServerAddress, 25);
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
            br = new BufferedReader(new InputStreamReader(this.in));
            br.readLine();
            printWriter = new PrintWriter(new OutputStreamWriter(out));
            System.out.println("[mailAgentService]-连接到" + mailServerAddress + "成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean login(String username, String password) {
        this.username = username;
        this.password = password;
        try {
            printWriter.print("HELO " + mailServerAddress + CRLF);
            printWriter.flush();
            response = br.readLine();
            printWriter.print("AUTH LOGIN\r\n");
            printWriter.flush();
            response = br.readLine();
            printWriter.print(Base64.getEncoder().encodeToString(username.getBytes()) + CRLF);
            printWriter.flush();
            response = br.readLine();
            printWriter.print(Base64.getEncoder().encodeToString(password.getBytes()) + CRLF);
            printWriter.flush();
            response = br.readLine();
            if (!response.equals("235 Authentication successful"))
                return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    boolean sendMail(Map<String, String> params) {
        //todo:目标地址、subject、content
        try {
            printWriter.print("MAIL FROM:<" + username + "@" + this.postFix + ">" + CRLF);
            printWriter.flush();
            response = br.readLine();
            printWriter.print("rcpt to:<" + params.get("tousername") + ">" + CRLF);
            printWriter.flush();
            response = br.readLine();
            if (!response.toLowerCase().contains("ok"))
                return false;
            printWriter.write("data" + CRLF);
            printWriter.flush();
            response = br.readLine();
            String data = "from:" + username + "@" + this.postFix + CRLF + "to:<" + params.get("tousername") + ">" + CRLF + "subject:" + String.valueOf(URLDecoder.decode(params.get("subject"), StandardCharsets.UTF_8)) + CRLF + CRLF + String.valueOf(URLDecoder.decode(params.get("content"), StandardCharsets.UTF_8)) + CRLF + "." + CRLF;
            printWriter.write(data);
            printWriter.flush();
            System.out.println(String.format("[mailAgentService]-邮件发送成功！From:%s:To:%s!", username + "@" + this.postFix, params.get("tousername")));
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    String queryMail(Map<String, String> params) {
        boolean state = popLogin(params);
        if (state) {
            return getMails(params);
        }
        return null;
    }

    private String getMails(Map<String, String> params) {
        jsonObject = new JSONObject();
        List<Map<String, String>> mailList = new ArrayList<>();
        int count = 0;
        printWriter.print("stat" + CRLF);
        printWriter.flush();
        count = count();
        jsonObject.put("totalCount", count);
        int start = Integer.parseInt(params.getOrDefault("startIndex", "1"));
        int end = Integer.parseInt(params.getOrDefault("endIndex", "10"));
        if (params.containsKey("exactIndex")) {
            mailList.add(retrieveMail(Integer.parseInt(params.get("exactIndex"))));
        } else {
            for (int i = start; i <= end; i++) {
                mailList.add(retrieveMail(i));
            }
        }
        jsonObject.put("data", mailList);
        return jsonObject.toString();
    }

    private boolean popLogin(Map<String, String> params) {
        try {
            socket = new Socket("pop." + postFix, 110);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(socket.getOutputStream());
            response = br.readLine();
            if (!response.startsWith("+OK")) {
                socket.close();
                return false;
            }
            executeCommand("user " + params.get("username"));
            executeCommand("pass " + params.get("password"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    boolean deleteMail(Map<String, String> params) {
        boolean state = popLogin(params);
        try {
            if (params.containsKey("deleteIndex")) {
                printWriter.print("dele " + params.get("deleteIndex") + CRLF);
                printWriter.flush();
                String line = br.readLine();
                if (!line.toLowerCase().contains("ok")){
                    return false;
                }
                printWriter.print("quit" + CRLF);
                printWriter.flush();
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    private int count() {
        int c = 0;
        try {
            String[] mail = br.readLine().split(" ");
            c = Integer.parseInt(mail[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return c;
    }

    private void executeCommand(String command) {
        try {
            printWriter.print(command + CRLF);
            printWriter.flush();
            response = br.readLine();
        } catch (IOException e) {
            //todo:读取时的错误处理过程
        }
    }

    private Map<String, String> retrieveMail(int index) {
        try {
            Map<String, String> mailInfoList = new HashMap<>();
            mailInfoList.put("index", String.valueOf(index));
            printWriter.print("retr " + index + CRLF);
            printWriter.flush();
            String response = br.readLine();
            String tempStr = "";
            String key = null, value = null;
            while (true) {
                if (response.contains(":") && response.split(":").length > 1) {
                    key = response.split(":")[0];
                    value = response.split(":")[1];
                    while (!(tempStr = br.readLine()).contains(":")) {
                        if (tempStr.equals("")) {
                            break;
                        }
                        value += tempStr;
                    }
                    response = tempStr;
                    mailInfoList.put(key, MimeUtility.decodeText(value));
                } else if (!response.contains(":") && !response.toLowerCase().contains("+ok")) {
                    while (!(tempStr = br.readLine()).equals(".")) {
                        response += tempStr;
                    }
                    byte[] bytes = response.getBytes();
                    mailInfoList.put("Content", new String(bytes, StandardCharsets.UTF_8));
                } else {
                    response = br.readLine();
                }
                if (tempStr.equals(".")) {
                    break;
                }
            }
            return mailInfoList;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
