package tech.rsqn.build.ant;


import java.io.*;
import java.util.*;


public class CommandLineInterface {
    private static String OS = System.getProperty("os.name").toLowerCase();

    private String executableToRun;
    private List<String> argumentsToPass;
    private Map<String,String> environmentVariables;
    private String logFilePath;
    private File workDir;

    public void setExecutableToRun(String executableToRun) {
        this.executableToRun = executableToRun;
    }

    public void setArgumentsToPass(List<String> argumentsToPass) {
        this.argumentsToPass = argumentsToPass;
    }

    public void addEnvironmentVariable(String name, String value) {
        environmentVariables.put(name, value);
    }

    public void setWorkDir(File workDir) {
        this.workDir = workDir;
    }

    private List<String> getShell() {
        List<String> ret = new ArrayList<String>();
        if ( isWindows() ) {
            ret.add("cmd.exe");
            ret.add("/c");
        } else {
//            ret.add("/bin/bash");
        }
        return ret;
    }


    public CommandLineInterface() {
        argumentsToPass = new ArrayList<String>();
        environmentVariables = new HashMap<String,String>();
        environmentVariables = System.getenv();
    }


    public void run() throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();

        List<String> commandAndArguments =  new ArrayList<String>();
        commandAndArguments.addAll(getShell());
        commandAndArguments.add(executableToRun);
        commandAndArguments.addAll(argumentsToPass);

        System.out.println("CommandLineInterface starting");
        System.out.println("Environment Variables...");
        for (String s : environmentVariables.keySet()) {
            System.out.println("   " + s + "=" + environmentVariables.get(s));
        }
        System.out.println("Executing Command And Arguments = " + commandAndArguments);
        System.out.println("WorkDir Absolute Path = " + workDir.getAbsolutePath());


        System.out.println("Launch Command ( " + commandAndArguments + ")");
        ProcessBuilder pb =new ProcessBuilder(commandAndArguments);
        Map<String, String> env = pb.environment();
        env.putAll(environmentVariables);
        pb.directory(workDir);
        Process p = pb.start();

        InputStream is = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;

        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }

        try {
            int exitValue = p.waitFor();
            System.out.println("\n\nExit Value is " + exitValue);
            if ( exitValue != 0) {
                throw new RuntimeException("NonZero exit value " + exitValue);
            }
        } catch (InterruptedException e) {
            System.err.println("Caught Exception waiting for process to finish " + e);
            throw e;
        }

        System.out.println("Process Complete");

    }

    public static boolean isWindows() {
        return (OS.contains("win"));
    }

    public static boolean isMac() {
        return (OS.contains("mac") );
    }

    public static boolean isUnix() {
        return (OS.contains("nix")|| OS.contains("nux")  || OS.contains("aix") );
    }

    public static boolean isSolaris() {
        return (OS.contains("sunos"));
    }
}
