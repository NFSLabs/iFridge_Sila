package sila_java.servers.ifridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class CheckOS {
    public boolean isWindows = false;
    public boolean isLinux = false;
    public boolean isHpUnix = false;
    public boolean isPiUnix = false;
    public boolean isSolaris = false;
    public boolean isSunOS = false;
    public boolean archDataModel32 = false;
    public boolean archDataModel64 = false;

    public CheckOS(){
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("windows") >= 0) {
            isWindows = true;
        }
        if (os.indexOf("linux") >= 0) {
            isLinux = true;
        }
        if (os.indexOf("hp-ux") >= 0) {
            isHpUnix = true;
        }
        if (os.indexOf("hpux") >= 0) {
            isHpUnix = true;
        }
        if (os.indexOf("solaris") >= 0) {
            isSolaris = true;
        }
        if (os.indexOf("sunos") >= 0) {
            isSunOS = true;
        }
        if (System.getProperty("sun.arch.data.model").equals("32")) {
            archDataModel32 = true;
        }
        if (System.getProperty("sun.arch.data.model").equals("64")) {
            archDataModel64 = true;
        }
        if (isLinux) {
            final File file = new File("/etc", "os-release");
            try (FileInputStream fis = new FileInputStream(file);
                 BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
                String string;
                while ((string = br.readLine()) != null) {
                    if (string.toLowerCase().contains("raspbian")) {
                        if (string.toLowerCase().contains("name")) {
                            isPiUnix = true;
                            break;
                        }
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }
}


