package tech.rsqn.deploy.elasticbeanstalk.maven;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mandrewes
 * Date: Apr 27, 2010
 * To change this template use File | Settings | File Templates.
 */

@Mojo(name = "fauxreport")

public class DeploymentMojo extends AbstractMojo {
    private String warFile;
    private String versionLabel;
    private String applicationName;
    private String environmentName;
    private String bucketName;
    private String accessKey;
    private String secretKey;
    private String s3Endpoint;
    private String s3BucketRegion;
    private String elasticBeanstalkEndPoint;

    public void execute() throws MojoExecutionException {
        System.out.println("DeploymentTask Executing - warFile " + warFile);

        try {
            File warFileHandle = new File(warFile);
            versionLabel += "-ts-" + System.currentTimeMillis();
            String key = applicationName + "-" + versionLabel;

            final String accessKeyFinal = accessKey;
            final String secretKeyFinal = secretKey;

            AWSCredentials credentials = new AWSCredentials() {
                public String getAWSAccessKeyId() {
                    return accessKey;
                }

                public String getAWSSecretKey() {
                    return secretKey;
                }
            };
            System.out.println("Connecting to S3");
            AmazonS3 s3 = new AmazonS3Client(credentials);
            s3.setEndpoint(s3Endpoint);

            System.out.println("There are " + s3.listBuckets().size() + " s3 buckets");

            List<Bucket> buckets = s3.listBuckets();
            for (Bucket bucket : buckets) {
                String location = s3.getBucketLocation(bucket.getName());

                System.out.println("Bucket: " + bucket + " location " + location);
            }

            if (s3.doesBucketExist(bucketName)) {
                System.out.println("Bucket: " + bucketName + " exists");
            } else {
                System.out.println("Creating Bucket: " + bucketName + " at region " + s3BucketRegion);
                s3.createBucket(bucketName, s3BucketRegion);

            }
            System.out.println("...");
            System.out.println("");

            System.out.println("Connecting to Elastic beanstalk");
            AWSElasticBeanstalk elasticBeanstalk = new AWSElasticBeanstalkClient(credentials);
            elasticBeanstalk.setEndpoint(elasticBeanstalkEndPoint);

            List<EnvironmentDescription> environments = elasticBeanstalk.describeEnvironments().getEnvironments();
            System.out.println("There are " + environments.size() + " environments at endpoint " + elasticBeanstalkEndPoint);
            for (EnvironmentDescription environment : environments) {
                System.out.println("Environment: " + environment.getApplicationName() + " - " + environment.getEndpointURL());
            }

            List<ApplicationDescription> applications = elasticBeanstalk.describeApplications().getApplications();
            System.out.println("There are " + applications.size() + " applications at endpoint " + elasticBeanstalkEndPoint);
            for (ApplicationDescription application : applications) {
                System.out.println("Application : " + application.getApplicationName() + " last updated " + application.getDateUpdated());
            }

            System.out.println("...");
            System.out.println("");

            // Upload a WAR file to Amazon S3

            System.out.println("Uploading application to Amazon S3");
            
            PutObjectResult s3Result = s3.putObject(bucketName, key, warFileHandle);

            System.out.println("Uploaded application " + s3Result.getETag());

            System.out.println("...");
            System.out.println("");

            System.out.println("Create application version with uploaded application");

            CreateApplicationVersionRequest createApplicationRequest = new CreateApplicationVersionRequest();
            createApplicationRequest.setApplicationName(applicationName);
            createApplicationRequest.setVersionLabel(versionLabel);
            createApplicationRequest.setAutoCreateApplication(false);
            createApplicationRequest.setSourceBundle(new S3Location(bucketName, key));

            CreateApplicationVersionResult createApplicationVersionResult = elasticBeanstalk.createApplicationVersion(createApplicationRequest);
            System.out.println("Registered application version " + createApplicationVersionResult.getApplicationVersion());

            System.out.println("...");
            System.out.println("");


            System.out.println("Deploying new version " + createApplicationVersionResult.getApplicationVersion() + " to application " + applicationName + " environment " + environmentName);

            UpdateEnvironmentRequest updateEnvironmentRequest = new UpdateEnvironmentRequest();
            updateEnvironmentRequest.setVersionLabel(versionLabel);
            updateEnvironmentRequest.setEnvironmentName(environmentName);
            UpdateEnvironmentResult updateEnvironmentResult = elasticBeanstalk.updateEnvironment(updateEnvironmentRequest);

            System.out.println("Update application  " + updateEnvironmentResult);

            System.out.println("...");
            System.out.println("");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new MojoExecutionException("Deployment Failed " + e.getMessage(), e);
        }
    }

    public String getWarFile() {
        return warFile;
    }

    public void setWarFile(String warFile) {
        this.warFile = warFile;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getS3Endpoint() {
        return s3Endpoint;
    }

    public void setS3Endpoint(String s3Endpoint) {
        this.s3Endpoint = s3Endpoint;
    }

    public String getS3BucketRegion() {
        return s3BucketRegion;
    }

    public void setS3BucketRegion(String s3BucketRegion) {
        this.s3BucketRegion = s3BucketRegion;
    }

    public String getElasticBeanstalkEndPoint() {
        return elasticBeanstalkEndPoint;
    }

    public void setElasticBeanstalkEndPoint(String elasticBeanstalkEndPoint) {
        this.elasticBeanstalkEndPoint = elasticBeanstalkEndPoint;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }
}
