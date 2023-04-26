package iudx.resource.server.database.async.util;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class S3FileOpsHelper {

  private static final Logger LOGGER = LogManager.getLogger(S3FileOpsHelper.class);
  static FileInputStream fileInputStream;
  private final Regions clientRegion;
  private final String bucketName;

  public S3FileOpsHelper(Regions clientRegion, String bucketName) {
    this.clientRegion = clientRegion;
    this.bucketName = bucketName;
  }

  private ClientConfiguration getClientConfiguration() {
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setRetryPolicy(new RetryPolicy(null, null, 3, false));
    return clientConfiguration;
  }

  public void s3Upload(File file, String objectKey, Handler<AsyncResult<JsonObject>> handler) {

    DefaultAWSCredentialsProviderChain credentialProviderChain =
        new DefaultAWSCredentialsProviderChain();

    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      AmazonS3 s3Client =
          AmazonS3ClientBuilder.standard()
              .withRegion(clientRegion)
              .withCredentials(credentialProviderChain)
              .withClientConfiguration(getClientConfiguration())
              .build();

      TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3Client).build();
      ObjectMetadata objectMetadata = new ObjectMetadata();
      objectMetadata.setContentDisposition("attachment; filename=" + file.getName());
      objectMetadata.setContentLength(file.length());
      // TransferManager processes all transfers asynchronously,
      // so this call returns immediately.
      Upload upload = tm.upload(bucketName, objectKey, fileInputStream, objectMetadata);
      LOGGER.info("Object upload started");
      // upload.addProgressListener(uploadProgressListener);
      upload.waitForCompletion();

      LOGGER.info("Object upload complete");
      ZonedDateTime zdt = ZonedDateTime.now();
      zdt = zdt.plusDays(1);
      Long expiry = zdt.toEpochSecond() * 1000;
      JsonObject result =
          new JsonObject()
              .put("s3_url", generatePreSignedUrl(expiry, objectKey))
              .put("expiry", zdt.toLocalDateTime().toString())
              .put("object_id", objectKey);
      handler.handle(Future.succeededFuture(result));
    } catch (AmazonServiceException e) {
      // The call was transmitted successfully, but Amazon S3 couldn't process
      // it, so it returned an error response.
      LOGGER.error(e);
      LOGGER.error(e.getErrorCode());
      LOGGER.error(e.getErrorMessage());
      LOGGER.error(e.getErrorType());
      handler.handle(Future.failedFuture(e));
    } catch (AmazonClientException e) {
      LOGGER.error(e);
      LOGGER.error(e.getCause());
      handler.handle(Future.failedFuture(e));
    } catch (InterruptedException e) {
      LOGGER.error(e);
      handler.handle(Future.failedFuture(e));
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      handler.handle(Future.failedFuture(e));
    } catch (IOException e) {
      LOGGER.error("unable to access file");
      LOGGER.error(e);
    }
  }

  public URL generatePreSignedUrl(long expiryTimeMillis, String objectKey) {

    URL url = null;
    DefaultAWSCredentialsProviderChain credentialProviderChain =
        new DefaultAWSCredentialsProviderChain();

    try {

      // Set the presigned URL to expire after one hour.
      LOGGER.debug("expiry : " + expiryTimeMillis);
      Date expiration = new Date();
      expiration.setTime(expiryTimeMillis);

      // Generate the presigned URL.
      LOGGER.debug("Generating pre-signed URL.");

      GeneratePresignedUrlRequest generatePresignedUrlRequest =
          new GeneratePresignedUrlRequest(bucketName, objectKey)
              .withMethod(HttpMethod.GET)
              .withExpiration(expiration);
      AmazonS3 s3Client =
          AmazonS3ClientBuilder.standard()
              .withRegion(clientRegion)
              .withCredentials(credentialProviderChain)
              .build();

      url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

      LOGGER.debug("Pre-Signed URL: " + url.toString());

    } catch (AmazonServiceException e) {
      LOGGER.error(e);
    } catch (SdkClientException e) {
      LOGGER.error(e);
    }
    return url;
  }
}
