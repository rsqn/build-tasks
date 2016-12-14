package tech.rsqn.build.ant.tools;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class MavenDependencyScanner {

    public static void main(String[] args) {
        try {
            MavenDependencyScanner scanner = new MavenDependencyScanner();
            scanner.run(new File(args[0]));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }


    public MavenDependencyScanner() {

    }

    private void run(File tld) throws Exception {
        System.out.println("scanning " + tld.getAbsolutePath());
        List<File> pomFiles = new ArrayList<File>();
        findPomFiles(pomFiles, tld);


        List<Dependency> allDependencies = new ArrayList<Dependency>();
        for (File file : pomFiles) {
            allDependencies.addAll(extractDependencyElements(file));
        }

        System.out.println("initial dependencies " + allDependencies.size());
        cleanItemsWithVersions(allDependencies);
        System.out.println("after items that have no version (good) " + allDependencies.size());
        cleanItemsWithExpressions(allDependencies);
        System.out.println("after cleaned expressions " + allDependencies.size());
        cleanItemsWithExcludes(allDependencies);
        System.out.println("after cleaned with excludes " + allDependencies.size());


        Set<Dependency> depDuplicated = new HashSet<Dependency>();
        for (Dependency d : allDependencies) {
            depDuplicated.add(d);
        }
        System.out.println("after de-duplicate " + depDuplicated.size());

        List<Dependency> sorted = new ArrayList<Dependency>();
        sorted.addAll(depDuplicated);
        Collections.sort(sorted);
        for (Dependency d : sorted) {
//            depDuplicated.add(d);
            System.out.println(d.toXml());
        }
    }

    private void cleanItemsWithVersions(List<Dependency> dependencies) {
        List<Dependency> dirty = new ArrayList<Dependency>(dependencies);
        dependencies.clear();
        for (Dependency d : dirty) {
            if ( d.version == null ) {
                continue;
            }
            dependencies.add(d);
        }
    }

    private void cleanItemsWithExpressions(List<Dependency> dependencies) {
        List<Dependency> dirty = new ArrayList<Dependency>(dependencies);
        dependencies.clear();
        for (Dependency d : dirty) {
            if ( d.groupId.contains("${")) {
                continue;
            }
            dependencies.add(d);
        }
    }

    /**
     * I'll move by hand
     * @param dependencies
     */
    private void cleanItemsWithExcludes(List<Dependency> dependencies) {
        List<Dependency> dirty = new ArrayList<Dependency>(dependencies);
        dependencies.clear();
        for (Dependency d : dirty) {
            if ( d.excludes != null ) {
                continue;
            }
            dependencies.add(d);
        }
    }

    private void findPomFiles(List<File> pomFiles, File tld) throws Exception {
        File[] files = tld.listFiles();

        for (File file : files) {
            if (file.getName().equals("pom.xml")) {
                System.out.println("adding " + file.getAbsolutePath());
                pomFiles.add(file);
            } else if (file.isDirectory()) {
                if ( file.getAbsolutePath().contains("integration")) {
                    if ( ! file.getAbsolutePath().contains("test")) {
                        continue;
                    }
                }
                findPomFiles(pomFiles, file);
            }
        }

    }

    private List<Dependency> extractDependencyElements(File f) throws Exception {
        List<Dependency> ret = new ArrayList<Dependency>();

        String s = FileUtils.readFileToString(f);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        Document doc = dBuilder.parse(f);

        NodeList list = doc.getElementsByTagName("dependency");

        for (int i = 0; i < list.getLength(); i++) {
            Dependency dep = new Dependency();
            dep.fromNode(list.item(i));
            ret.add(dep);
//            System.out.println(dep);
        }

        return ret;
    }


    private class Dependency implements Comparable {
        String groupId;
        String artifactId;
        String version;
        String scope;
        String type;
        String excludes;

        @Override
        public int compareTo(Object o) {
            Dependency other = ((Dependency) o);
            String otherName = other.groupId + "." + other.artifactId + "." + other.version;
            String myName = groupId + "." + artifactId + "." + version;

            if ( scope != null && other.scope == null) {
                return 1;
            }

            if ( scope == null && other.scope != null) {
                return -1;
            }

            return myName.compareTo(otherName);
        }

        private Node find(String name, Node node) {
            NodeList l = node.getChildNodes();
            for (int i = 0; i < l.getLength(); i++) {
                Node item = l.item(i);
                if ( item.getNodeName().equals(name)) {
                    return item;
                }
            }
            return null;
        }
        public void fromNode(Node node) {
            groupId = find("groupId",node).getTextContent();
            artifactId = find("artifactId", node).getTextContent();

            if ( find("version",node) != null ) {
                version = find("version",node).getTextContent();
            }
            if ( find("scope",node) != null ) {
                scope = find("scope",node).getTextContent();
            }
            if ( find("type",node) != null ) {
                type = find("type",node).getTextContent();
            }
            if ( find("excludes",node) != null ) {
                excludes = find("excludes",node).getTextContent();
            }
        }

        public String toXml() {
            String ret = "        <dependency>\n" +
                    "            <groupId>" + groupId + "</groupId>\n" +
                    "            <artifactId>" + artifactId + "</artifactId>\n" +
                    "            <version>" + version + "</version>\n";
            if ( type != null) {
                ret += "            <type>" + type + "</type>\n";
            }
            if ( scope != null) {
                ret += "            <scope>" + scope + "</scope>\n";
            }
            ret += "        </dependency>";
            return ret;
        }

        @Override
        public String toString() {
            return "Dependency{" +
                    "groupId='" + groupId + '\'' +
                    ", artifactId='" + artifactId + '\'' +
                    ", version='" + version + '\'' +
                    ", scope='" + scope + '\'' +
                    ", type='" + type + '\'' +
                    ", excludes='" + excludes + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Dependency that = (Dependency) o;

            if (!artifactId.equals(that.artifactId)) return false;
            if (excludes != null ? !excludes.equals(that.excludes) : that.excludes != null) return false;
            if (!groupId.equals(that.groupId)) return false;
            if (scope != null ? !scope.equals(that.scope) : that.scope != null) return false;
            if (type != null ? !type.equals(that.type) : that.type != null) return false;
            if (version != null ? !version.equals(that.version) : that.version != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = groupId.hashCode();
            result = 31 * result + artifactId.hashCode();
            result = 31 * result + (version != null ? version.hashCode() : 0);
            result = 31 * result + (scope != null ? scope.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (excludes != null ? excludes.hashCode() : 0);
            return result;
        }
    }
}
