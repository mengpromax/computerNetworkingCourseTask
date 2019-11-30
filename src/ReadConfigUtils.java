import java.io.*;
import java.util.HashMap;
import java.util.Map;

class ReadConfigUtils {
    static Map<String,String> configList = new HashMap<>();
    private static String ROOT = System.getProperty("user.dir");
    static Map<String,String> getConfigList(String fileName){
        if (fileName == null){
            fileName = "serverConfig.properties";
        }
        String configFilePath = ROOT + "/" + fileName;
        try {
            InputStream inputStream = new FileInputStream(new File(configFilePath));
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while((line = br.readLine()) != null){
                if(line.contains("="))
                    configList.put(line.split("=")[0],line.split("=")[1]);
            }
            return configList;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configList;
    }
}
