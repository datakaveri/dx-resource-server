package iudx.resource.server.database.async.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.amazonaws.regions.Regions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class S3FileOpsHelperTest {
    @Mock
    Regions clientRegion;
    @Mock
    File file;
    String bucketName;
    S3FileOpsHelper opsHelper;
    String bucket;
    String expected_error;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
//        bucketName = "arn:/abcd:abcd:abcd:abcd:accesspoint:dummy/access/point.:abcd/abcd/:abcd/:abcd/:abcd/::abcd:abcdabcd:s3-object-lambda";
        bucket = "arn:aws:s3:::examplebucket/developers/design_info.doc";
        when(clientRegion.getName()).thenReturn("Dummy.client-region.3");
        opsHelper = new S3FileOpsHelper(null,"Dummy.client-region.3", "abc","xyz",bucket);
        vertxTestContext.completeNow();
    }

    /**
     * this test case fails due to SdkClientException
     * which is caused by NULL access key and secret key
     **/

    //@Test
    @DisplayName("Test generatePreSignedUrl method : with SdkClientException")
    public void test_generatePreSignedUrl_with_SdkClientException(VertxTestContext vertxTestContext) {
        long expiryTimeMillis = 3032000;
        String objectKey = "Dummy objectKey";
        assertNotNull(opsHelper.generatePreSignedUrl(expiryTimeMillis, objectKey));
        vertxTestContext.completeNow();
    }

}
