package tech.rsqn.deploy.elasticbeanstalk;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: mandrewes
 * Date: Apr 27, 2010
 * Time: 3:00:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class Utils {
    public static String readToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String ret ="";

        while (true)
        {
            String line = br.readLine();
            if (line == null)
                break;
            ret += line + System.getProperty("line.separator");
        }

        return ret;
    }

    public static void copyFile(String srFile, String dtFile) {
        File f1 = new File(srFile);
        File f2 = new File(dtFile);
        copyFile(f1, f2);
    }

    public static void copyFile(File f1, File f2) {
        try {

            InputStream in = new FileInputStream(f1);

            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
        catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage() + " in the specified directory.");
            System.exit(0);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static byte[] readFileToByteArray(File f1) {

        try {

            InputStream in = new FileInputStream(f1);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return out.toByteArray();
        }
        catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage() + " in the specified directory.");
            System.exit(0);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
