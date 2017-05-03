package tech.rsqn.build.ant.anttasks.build;

import org.testng.annotations.Test;
import tech.rsqn.build.naming.ant.NamedBuildNumber;

public class NamedBuilderNumberTest {

    @Test
    public void testName() throws Exception {
        NamedBuildNumber n = new NamedBuildNumber();
        n.setMajor("1");
        n.setMid("2");
        n.setMinor("3");

        n.setHost("localhost");
        n.setName("Fred");
        n.setScmAuthor("Fred");
        n.setScmCommittedDate("124356");
        n.setScmUri("revision");
        n.setScmRevision("revision");

        n.setSeqNo("5");

        System.out.println(n.toFileFormat());

        String ff = n.toFileFormat();

        NamedBuildNumber parsed = NamedBuildNumber.parse(ff);
    }

}
