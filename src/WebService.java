import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

abstract class WebService extends Thread{
    static String readApiInfo(InputStream in){
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String readLine = null;
        String[] split = null;
        try {
            readLine = reader.readLine();
            split = readLine.split(" ");
            if (split.length != 3)
                return null;
            return split[1];
        } catch (Exception e) {
            //todo:传过来的格式不符合要iu
        }
        return null;
    }
}