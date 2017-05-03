package tech.rsqn.build.naming.ant;


import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public abstract class AbstractBuildNamingTask extends Task {

    private String buildNumberFile;
    private String buildNameFile;
    private String tasks;
    private List<String> tasksList;
    private String buildPropertiesDest;
    private boolean mayModifyBuildNumber = true;

    private static final String possibleOptions = "Possible task values are increment_build,commit_build_number,tag, update_pom_versions, commit_pom_files, copy_build_number, use_releases";

    protected File resolveBuildNumberFile() {
        File f;
        if (buildNumberFile.contains("/") || buildNumberFile.contains("\\")) {
            f = new File(buildNumberFile);
        } else {
            f = new File(getProject().getBaseDir(), buildNumberFile);
            if (!f.exists()) {
                File parent = new File(getProject().getBaseDir().getParentFile(), buildNumberFile);
                if (parent.exists()) {
                    mayModifyBuildNumber = false;
                    System.out.println("Did not find build number in local module but found in immediate parent");
                    f = parent;
                }
            }
        }

        System.out.println("Looking for build number file at path " + f.getAbsolutePath());
        return f;
    }

    protected File resolveBuildNameFile() {
        File f;
        if (buildNameFile.contains("/") || buildNameFile.contains("\\")) {
            f = new File(buildNameFile);
        } else {
            f = new File(getProject().getBaseDir(), buildNameFile);
            if (!f.exists()) {
                File parent = new File(getProject().getBaseDir().getParentFile(), buildNameFile);
                if (parent.exists()) {
                    mayModifyBuildNumber = false;
                    System.out.println("Did not find build name in local module but found in immediate parent");
                    f = parent;
                }
            }
        }
        System.out.println("Looking for build name file at path " + f.getAbsolutePath());
        return f;
    }

    protected NamedBuildNumber getCurrentBuildNumber() {
        String s = null;
        try {
            s = FileUtils.readFileToString(resolveBuildNumberFile());
            return NamedBuildNumber.parse(s);
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
    }


    protected void incrementVersionNumbers(NamedBuildNumber n) {
        int minor = Integer.parseInt(n.getMinor());
        int mid = Integer.parseInt(n.getMid());
        int major = Integer.parseInt(n.getMajor());
        int seqNo = Integer.parseInt(n.getSeqNo());
        minor++;
        seqNo++;
        n.setMajor("" + major);
        n.setMid("" + mid);
        n.setMinor("" + minor);
        n.setSeqNo("" + seqNo);
    }

    protected String resolveFromNameLine(String n, String[] parts) {
        for (String part : parts) {
            if (part.trim().startsWith(n + "=")) {
                return part.trim().substring(n.length() + 1).trim();
            }
        }
        return "";
    }

    public static String resolveMavenExecutable() {
        String m2Home = System.getenv("M2_HOME");
        if (m2Home == null) {
            m2Home = "/usr/local/";
//            throw new RuntimeException("Could not find M2_HOME in environment properties");
        }

        File cmdFile;
        if (CommandLineInterface.isWindows()) {
            cmdFile = new File(new File(m2Home, "bin"), "mvn.bat");
        } else {
            cmdFile = new File(new File(m2Home, "bin"), "mvn");
        }

        System.out.println("I have decided that the maven executable is at " + cmdFile.getAbsolutePath());

        if (!cmdFile.exists()) {
            throw new RuntimeException("Could not find maven exeuctable at " + cmdFile.getAbsolutePath());
        }

        return cmdFile.getAbsolutePath();
    }

    protected void resolveBuildName(NamedBuildNumber bn) {
        String buildNameContents = null;
        try {
            buildNameContents = FileUtils.readFileToString(resolveBuildNameFile());
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        String[] nameLines = buildNameContents.split("\n");
        List<String> notBlank = new ArrayList<String>();
        for (String nameLine : nameLines) {
            if (nameLine.trim().length() > 0) {
                notBlank.add(nameLine.trim());
            }
        }
        int index = Integer.parseInt(bn.getSeqNo()) % notBlank.size();
        String line = notBlank.get(index);
        String[] parts = line.split(",");
        bn.setName(parts[0].trim());
        bn.setDescription(resolveFromNameLine("description", parts));
    }

    protected void resolveEnvironmentalSettings(NamedBuildNumber bn) throws SocketException {
        bn.setHost(Utils.getLocalHostName());
    }

    protected abstract void createTag(NamedBuildNumber nb, boolean tryWithAuthProperties);

    protected abstract void commitBuildNumber(NamedBuildNumber nb, boolean tryWithAuthProperties);

    protected abstract void resolveSubversionInformation(NamedBuildNumber bn, boolean tryWithAuthProperties);

    protected NamedBuildNumber getIncremetedBuildNumber(NamedBuildNumber current) throws CloneNotSupportedException {
        NamedBuildNumber next = (NamedBuildNumber) current.clone();
        incrementVersionNumbers(next);
        next.setBuildTime(new Date().toString());
        return next;
    }

    public void execute() throws BuildException {
        System.out.println("SvnBuildNamingTask Executing " + this);
        System.out.println(possibleOptions);

        try {
            tasksList = new ArrayList<String>();
            Collections.addAll(tasksList, tasks.toLowerCase().split(","));
            System.out.println("tasks requested are " + tasksList);
            NamedBuildNumber current = getCurrentBuildNumber();

            System.out.println("Current Build Number " + current.toString());
            NamedBuildNumber buildNumber = current;


            if (tasks.contains("mvn_validate")) {
                mavenValidate(buildNumber);
            }

            if (tasks.contains("increment_build")) {
                System.out.println("Incrementing build number");
                if (!mayModifyBuildNumber) {
                    throw new BuildException("This module has no local build number - modification is not allowed");
                }
                NamedBuildNumber next = getIncremetedBuildNumber(current);
                resolveBuildName(next);
                resolveEnvironmentalSettings(next);
                resolveSubversionInformation(next, false);
                System.out.println("This Build Number " + next.toString());
                FileUtils.writeStringToFile(resolveBuildNumberFile(), next.toFileFormat());
                buildNumber = next;
            }

            if (tasks.contains("copy_build_number")) {
                System.out.println("Copy Build Number File...");
                copyBuildProperties();
            }

            if (tasks.contains("commit_build_number")) {
                System.out.println("Committing build number...");
                commitBuildNumber(buildNumber, false);
            }

            if (tasks.contains("update_pom_versions")) {
                updatePomFileVersions(buildNumber);
            }

            if (tasks.contains("use_releases")) {
                useReleases(buildNumber);
            }

            if (tasks.contains("commit_pom_files")) {
                System.out.println("Committing Pom Files...");
                commitPomFiles(buildNumber, false);
            }

            if (tasks.contains("tag")) {
                System.out.println("Creating tag...");
                createTag(buildNumber, false);
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new BuildException("Exception in build " + e, e);
        }

    }

    protected void copy(File source, File destination) throws IOException {
        long length = source.length();
        FileChannel input = new FileInputStream(source).getChannel();
        FileChannel output = new FileOutputStream(destination).getChannel();
        input.transferTo(0, length, output);
        output.close();
        input.close();
    }

    protected void copyBuildProperties() throws IOException {
        File f = resolveBuildNumberFile();
        File target = new File(getProject().getBaseDir(), buildPropertiesDest);

        System.out.println("Source BuildNumber file is " + f.getAbsolutePath());
        System.out.println("Target BuildNumber file is " + target.getAbsolutePath());
        if (!f.exists()) {
            System.out.println("Parent Module build properties not found at " + f.getAbsolutePath());
            throw new BuildException("Parent Module build properties not found at " + f.getAbsolutePath());
        }


        if (!target.getParentFile().exists()) {
            System.out.println("Creating parent directory for " + target.getAbsolutePath());
            target.getParentFile().mkdirs();
        }
        if (target.isFile()) {
            copy(f, target);
            return;
        }

        if (target.createNewFile()) {
            copy(f, target);
        } else {
            System.out.println("Target buildnumber directory does not exist " + target.getAbsolutePath());
            throw new BuildException("Target buildnumber directory does not exist " + target.getAbsolutePath());
        }

    }


    protected void mavenValidate(NamedBuildNumber buildNumber) {
        try {
            // see javadoc of CommandLineInterface, I tried MavenCli with no luck.
            CommandLineInterface cli = new CommandLineInterface();
            cli.setExecutableToRun(resolveMavenExecutable());
            cli.setArgumentsToPass(Lists.newArrayList("validate"));
            cli.setWorkDir(getProject().getBaseDir());
            cli.run();

            /*
            MavenCli cli = new MavenCli();
            File f = new File("");
            System.out.println(f.getAbsolutePath());
            int result = cli.doMain(new String[]{"validate"},f.getAbsolutePath(),System.out, System.out);
            System.out.println("result: " + result);
            */
        } catch (Exception ex) {
            System.out.println("Caught exception running maven validate");
            ex.printStackTrace();
            throw new RuntimeException("Exception running mavencli " + ex, ex);
        }
    }

    protected void updatePomFileVersions(NamedBuildNumber buildNumber) throws IOException, InterruptedException {
        try {
            // see javadoc of CommandLineInterface, I tried MavenCli with no luck.
            CommandLineInterface cli = new CommandLineInterface();
            cli.setExecutableToRun(resolveMavenExecutable());
            cli.setArgumentsToPass(Lists.newArrayList("versions:set", "-DnewVersion=" + buildNumber.toVersionString()));
            cli.setWorkDir(getProject().getBaseDir());
            cli.run();

            cli.setExecutableToRun(resolveMavenExecutable());
            cli.setArgumentsToPass(Lists.newArrayList("versions:commit"));
            cli.setWorkDir(getProject().getBaseDir());
            cli.run();
        } catch (Exception ex) {
            System.out.println("Caught exception running maven commands " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Exception running maven commands " + ex, ex);
        }
    }

    protected void useReleases(NamedBuildNumber buildNumber) throws IOException, InterruptedException {
        try {
            // see javadoc of CommandLineInterface, I tried MavenCli with no luck.
            CommandLineInterface cli = new CommandLineInterface();
            cli.setExecutableToRun(resolveMavenExecutable());
            cli.setArgumentsToPass(Lists.newArrayList("versions:use-releases"));
            cli.setWorkDir(getProject().getBaseDir());
            cli.run();

            cli.setExecutableToRun(resolveMavenExecutable());
            cli.setArgumentsToPass(Lists.newArrayList("versions:commit"));
            cli.setWorkDir(getProject().getBaseDir());
            cli.run();
        } catch (Exception ex) {
            System.out.println("Caught exception running maven commands " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Exception running maven commands " + ex, ex);
        }
    }

    protected abstract void commitPomFiles(NamedBuildNumber nb, boolean tryWithAuthProperties);

    protected void findPomFiles(File dir, List<File> files) {
        File[] listing = dir.listFiles();

        for (File file : listing) {
            if (file.isDirectory()) {
                findPomFiles(file, files);
            } else {
                if (file.getName().equals("pom.xml")) {
                    files.add(file);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "AbstractBuildNamingTask{" +
                "buildNumberFile='" + buildNumberFile + '\'' +
                ", buildNameFile='" + buildNameFile + '\'' +
                ", tasks='" + tasks + '\'' +
                ", tasksList=" + tasksList +
                '}';
    }


    public String getBuildNumberFile() {
        return buildNumberFile;
    }

    public void setBuildNumberFile(String buildNumberFile) {
        this.buildNumberFile = buildNumberFile;
    }

    public String getBuildNameFile() {
        return buildNameFile;
    }

    public void setBuildNameFile(String buildNameFile) {
        this.buildNameFile = buildNameFile;
    }

    public String getTasks() {
        return tasks;
    }

    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    public List<String> getTasksList() {
        return tasksList;
    }

    public void setTasksList(List<String> tasksList) {
        this.tasksList = tasksList;
    }

    public String getBuildPropertiesDest() {
        return buildPropertiesDest;
    }

    public void setBuildPropertiesDest(String buildPropertiesDest) {
        this.buildPropertiesDest = buildPropertiesDest;
    }

    public boolean isMayModifyBuildNumber() {
        return mayModifyBuildNumber;
    }

    public void setMayModifyBuildNumber(boolean mayModifyBuildNumber) {
        this.mayModifyBuildNumber = mayModifyBuildNumber;
    }

    public static String getPossibleOptions() {
        return possibleOptions;
    }
}
