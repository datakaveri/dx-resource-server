package iudx.resource.server.database.elastic;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestEsResponseFormatterToJson {
    @Mock
    List<Hit<ObjectNode>> searchHits;
    private EsResponseFormatterToJson responseFormatterToJson;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext)
    {
        Configuration configuration = new Configuration();
        JsonObject config = configuration.configLoader(8, Vertx.vertx());
        String filePath = config.getString("filePath") + "dummy_file" + ".json";
        File file = new File(filePath);
        responseFormatterToJson = new EsResponseFormatterToJson(file);
        responseFormatterToJson.fileWriter = mock(FileWriter.class);

        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test write method : Success")
    public void testWrite(VertxTestContext vertxTestContext) throws IOException {
        responseFormatterToJson.write(searchHits);
        verify(responseFormatterToJson.fileWriter,times(1)).write('[');
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test write method : Failure")
    public void testWriteFailure(VertxTestContext vertxTestContext) throws IOException {
        doThrow(IOException.class).when(responseFormatterToJson.fileWriter).write('[');
        assertThrows(RuntimeException.class, ()-> responseFormatterToJson.write(searchHits));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test finish method : Success")
    public void testFinish(VertxTestContext vertxTestContext) throws IOException {
        responseFormatterToJson.finish();
        verify(responseFormatterToJson.fileWriter,times(1)).write(']');
        verify(responseFormatterToJson.fileWriter,times(1)).close();
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test finish method : Failure")
    public void testFinishFailure(VertxTestContext vertxTestContext) throws IOException {
        doThrow(IOException.class).when(responseFormatterToJson.fileWriter).close();

        assertThrows(RuntimeException.class,()-> responseFormatterToJson.finish());

        verify(responseFormatterToJson.fileWriter,times(1)).write(']');
        verify(responseFormatterToJson.fileWriter,times(1)).close();
        vertxTestContext.completeNow();
    }
  @Test
  public void testAppend(VertxTestContext vertxTestContext) throws IOException {

    ObjectNode node1 = mock(ObjectNode.class);
    ObjectNode node2 = mock(ObjectNode.class);
    Hit<ObjectNode> hit1 = mock(Hit.class);
    Hit<ObjectNode> hit2 = mock(Hit.class);

    when(hit1.source()).thenReturn(node1);
    when(hit2.source()).thenReturn(node2);
    when(node1.toString()).thenReturn("node1");
    when(node2.toString()).thenReturn("node2");

    List<Hit<ObjectNode>> searchHits = Arrays.asList(hit1, hit2);

    responseFormatterToJson.append(searchHits);
    vertxTestContext.completeNow();
  }
}
