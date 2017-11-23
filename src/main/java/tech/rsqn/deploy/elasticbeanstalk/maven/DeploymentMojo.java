package tech.rsqn.deploy.elasticbeanstalk.maven;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mandrewes
 * Date: Apr 27, 2010
 * To change this template use File | Settings | File Templates.
 */

@Mojo(name = "deploy-elasticbeanstalk")

public class DeploymentMojo extends AbstractMojo {
    @Parameter
    private String artifactFile;
    @Parameter
    private String versionLabel;
    @Parameter
    private String applicationName;
    @Parameter
    private String environmentName;
    @Parameter
    private String bucketName;
    @Parameter
    private String accessKey;
    @Parameter
    private String secretKey;
    @Parameter
    private String region;

    public void execute() throws MojoExecutionException {
        System.out.println("DeploymentTask Executing - artifactFile " + artifactFile);

        try {
            File fileHandle = new File(artifactFile);
            versionLabel += "-ts-" + System.currentTimeMillis();
            String key = applicationName + "-" + versionLabel;

            System.out.println("Connecting to S3");

            AWSCredentialsProvider credentials = null;

            if ( accessKey != null && accessKey.trim().length() > 0 ) {
                System.out.println("Using provided accessKey and secretKey parameters");
                credentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
            } else {
                System.out.println("Using DefaultAWSCredentialsProviderChain");
                credentials = new DefaultAWSCredentialsProviderChain();
            }

            AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(credentials).build();

            System.out.println("There are " + s3.listBuckets().size() + " s3 buckets");

            List<Bucket> buckets = s3.listBuckets();
            for (Bucket bucket : buckets) {
                String location = s3.getBucketLocation(bucket.getName());
                System.out.println("Bucket: " + bucket + " location " + location);
            }

            if (s3.doesBucketExist(bucketName)) {
                System.out.println("Bucket: " + bucketName + " exists");
            } else {
                System.out.println("Creating Bucket: " + bucketName + " at region " + region);
                s3.createBucket(bucketName, region);

            }
            System.out.println("...");
            System.out.println("");

            System.out.println("Connecting to Elastic beanstalk");
            AWSElasticBeanstalk elasticBeanstalk = new AWSElasticBeanstalkClient(credentials);
            elasticBeanstalk.setRegion(Region.getRegion(Regions.fromName(region)));

            List<EnvironmentDescription> environments = elasticBeanstalk.describeEnvironments().getEnvironments();
            System.out.println("There are " + environments.size() + " environments ");
            for (EnvironmentDescription environment : environments) {
                System.out.println("Environment: " + environment.getApplicationName() + " - " + environment.getEndpointURL());
            }

            List<ApplicationDescription> applications = elasticBeanstalk.describeApplications().getApplications();
            System.out.println("There are " + applications.size() + " applications");
            for (ApplicationDescription application : applications) {
                System.out.println("Application : " + application.getApplicationName() + " last updated " + application.getDateUpdated());
            }

            System.out.println("...");
            System.out.println("");

            // Upload a WAR file to Amazon S3

            System.out.println("Uploading application to Amazon S3");
            
            PutObjectResult s3Result = s3.putObject(bucketName, key, fileHandle);

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
}
