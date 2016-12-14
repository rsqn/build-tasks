package tech.rsqn.build.ant.anttasks;

import com.google.common.collect.Lists;
import tech.rsqn.build.ant.AbstractBuildNamingTask;
import tech.rsqn.build.ant.CommandLineInterface;
import org.apache.maven.cli.MavenCli;
import org.junit.Test;

import java.io.File;

public class MavenCliTest {

    @Test
    public void testName() throws Exception {
        try {
            MavenCli cli = new MavenCli();
            File f = new File("");
            System.out.println(f.getAbsolutePath());
            int result = cli.doMain(new String[]{"validate"},f.getAbsolutePath(),System.out, System.out);
            System.out.println("result: " + result);
        } catch ( Exception ex) {
            throw ex;
        }

    }



    @Test
    public void testCustomCli() throws Exception {
        File workDir = new File(new File("").getAbsolutePath());

        // see javadoc of CommandLineInterface, I tried MavenCli with no luck.
        CommandLineInterface cli = new CommandLineInterface();
        cli.setExecutableToRun(AbstractBuildNamingTask.resolveMavenExecutable());
        cli.setArgumentsToPass(Lists.newArrayList("validate"));
//        cli.setExecutableToRun("/usr/bin/which");
//        cli.setArgumentsToPass(new ArrayList<String>("mvn"));
//                cli.setArgumentsToPass(Lists.newArrayList("mvn"));
        cli.setWorkDir(workDir);
        cli.run();

    }


}
