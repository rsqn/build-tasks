package tech.rsqn.build.naming.ant;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

public class NamedBuildNumber implements Cloneable {
    private String major;
    private String mid;
    private String minor;
    private String host;
    private String name;
    private String scmUri;
    private String scmRevision;
    private String scmAuthor;
    private String scmCommittedDate;
    private String seqNo;
    private String buildTime;
    private String description;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    private String nns(String s) {
        if ( s == null) {
            return "null";
        }
        return s;
    }

    public String toFileFormat() throws IOException {
        Properties props = new Properties();
        props.put("build.version", major + "." + mid + "." + minor);
        props.put("build.host", nns(host));
        props.put("build.name", nns(name));
        props.put("build.scmUri", nns(scmUri));
        props.put("build.scmRevision", nns(scmRevision));
        props.put("build.scmAuthor", nns(scmAuthor));
        props.put("build.scmCommittedDate", nns(scmCommittedDate));
        props.put("build.seqNo", nns(seqNo));
        props.put("build.buildTime", nns(buildTime));
        props.put("build.description", nns(description));
        props.put("build.versionString", nns(toVersionString()));

        StringWriter writer = new StringWriter();
        props.store(writer,"SvnBuildNamingTask");

        return writer.toString();
    }

    private static String findValue(String name, String[] toks) {
        for (String tok : toks) {
            tok = tok.trim();
            if (tok.startsWith(name)) {
                return tok.split("=")[1].trim();
            }
        }
        return "";
    }

    public String toShortEnglishName() {
        return major + "." + mid + "." + minor + " - " + name;
    }

    public String toSCMTagName() {
        return "RELEASE_" + major + "." + mid + "." + minor + "-" + name.replace(" ","_").toUpperCase();
    }

    public String toVersionString() {
        return major + "." + mid + "." + minor + "-" + name.replace(" ","_").toUpperCase();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static NamedBuildNumber parse(String s) throws IOException {
        Properties props = new Properties();
        props.load(new StringReader(s));

        NamedBuildNumber ret = new NamedBuildNumber();

        ret.major = ((String) props.get("build.version")).split("\\.")[0];
        ret.mid = ((String) props.get("build.version")).split("\\.")[1];
        ret.minor = ((String) props.get("build.version")).split("\\.")[2];

        ret.host = (String) props.get("build.host");
        ret.name = (String) props.get("build.name");
        ret.scmUri = (String) props.get("build.scmUri");
        ret.scmRevision = (String) props.get("build.scmRevision");
        ret.scmAuthor = (String) props.get("build.scmAuthor");
        ret.scmCommittedDate = (String) props.get("build.scmCommittedDate");
        ret.seqNo = (String) props.get("build.seqNo");
        ret.buildTime = (String) props.get("build.buildTime");
        ret.description = (String) props.get("build.description");

        return ret;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getMinor() {
        return minor;
    }

    public void setMinor(String minor) {
        this.minor = minor;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScmUri() {
        return scmUri;
    }

    public void setScmUri(String scmUri) {
        this.scmUri = scmUri;
    }

    public String getScmRevision() {
        return scmRevision;
    }

    public void setScmRevision(String scmRevision) {
        this.scmRevision = scmRevision;
    }

    public String getScmAuthor() {
        return scmAuthor;
    }

    public void setScmAuthor(String scmAuthor) {
        this.scmAuthor = scmAuthor;
    }

    public String getScmCommittedDate() {
        return scmCommittedDate;
    }

    public void setScmCommittedDate(String scmCommittedDate) {
        this.scmCommittedDate = scmCommittedDate;
    }

    public String getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(String seqNo) {
        this.seqNo = seqNo;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(String buildTime) {
        this.buildTime = buildTime;
    }

    @Override
    public String toString() {
        return "NamedBuildNumber{" +
                "major='" + major + '\'' +
                ", mid='" + mid + '\'' +
                ", minor='" + minor + '\'' +
                ", host='" + host + '\'' +
                ", name='" + name + '\'' +
                ", scmUri='" + scmUri + '\'' +
                ", scmRevision='" + scmRevision + '\'' +
                ", scmAuthor='" + scmAuthor + '\'' +
                ", scmCommittedDate='" + scmCommittedDate + '\'' +
                ", seqNo='" + seqNo + '\'' +
                ", buildTime='" + buildTime + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
