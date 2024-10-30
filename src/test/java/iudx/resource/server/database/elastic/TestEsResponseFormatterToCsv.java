package iudx.resource.server.database.elastic;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestEsResponseFormatterToCsv {
    @Mock
    FileWriter fileWriter;
    private EsResponseFormatterToCsv responseFormatterToCsv;
    private File file;
    private Configuration configuration;
    private JsonObject config;
    @Mock
    private List<Hit<ObjectNode>> searchHits;
    @Mock
    private Hit<ObjectNode> objectNodeHit;
    @Mock
    private ObjectNode objectNode;
    private Set<String> stringSet;
    private JsonFlatten jsonFlatten;
    @Mock
    private Map<String, Object> map;
    private LinkedHashMap<String, Object> stringObjectMap;


    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext)
    {
        configuration = new Configuration();
        config = configuration.configLoader(8, Vertx.vertx());
        String filePath = config.getString("filePath") + "dummy_file" + "." + "csv";
        file = new File(filePath);
        EsResponseFormatterToCsv.jsonFlatten = mock(JsonFlatten.class);
        jsonFlatten = EsResponseFormatterToCsv.jsonFlatten;
        EsResponseFormatterToCsv.map = mock(LinkedHashMap.class);
        stringObjectMap = EsResponseFormatterToCsv.map;
        stringSet = new HashSet<>(Arrays.asList("header1" ,"header2", "header3", "header4"));
        lenient().when(searchHits.get(anyInt())).thenReturn(objectNodeHit);
        lenient().when(objectNodeHit.source()).thenReturn(objectNode);
        lenient().when(jsonFlatten.flatten()).thenReturn(stringObjectMap);
        lenient().when(stringObjectMap.keySet()).thenReturn(stringSet);
        responseFormatterToCsv = new EsResponseFormatterToCsv(file);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test method : Success")
    public void testWriteMethod(VertxTestContext vertxTestContext)
    {
        responseFormatterToCsv.writeToCsv(searchHits);
        verify(searchHits, times(1)).get(anyInt());
        verify(jsonFlatten,times(1)).flatten();
        verify(stringObjectMap,times(1)).keySet();
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test method : Failure")
    public void testWriteMethodFailure(VertxTestContext vertxTestContext) throws IOException {
        responseFormatterToCsv.writeToCsv(searchHits);
        responseFormatterToCsv.fileWriter = mock(fileWriter.getClass());
        doThrow(new IOException()).when(responseFormatterToCsv.fileWriter).write(anyString());

        verify(searchHits, times(1)).get(anyInt());
        verify(jsonFlatten,times(1)).flatten();
        verify(stringObjectMap,times(1)).keySet();

        assertThrows(RuntimeException.class,()-> responseFormatterToCsv.writeToCsv(searchHits));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test finish method : Success")
    public void testFinish(VertxTestContext vertxTestContext) throws IOException {
        responseFormatterToCsv.fileWriter = mock(fileWriter.getClass());
        responseFormatterToCsv.finish();
        verify(responseFormatterToCsv.fileWriter, times(1)).close();
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test finish method : Failure")
    public void testFinishFailure(VertxTestContext vertxTestContext) throws IOException {
        responseFormatterToCsv.fileWriter = mock(fileWriter.getClass());
        doThrow(new IOException()).when(responseFormatterToCsv.fileWriter).close();
        assertThrows(RuntimeException.class,()-> responseFormatterToCsv.finish());
        verify(responseFormatterToCsv.fileWriter, times(1)).close();
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test appendToCsvFile method : Success")
    public void testAppend(VertxTestContext vertxTestContext)
    {
        when(map.get(anyString())).thenReturn("dummy");
        responseFormatterToCsv.appendToCsvFile(map,stringSet);
        verify(map, times(stringSet.size())).get(anyString());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test appendToCsvFile method : Failure")
    public void testAppendFailure(VertxTestContext vertxTestContext) throws IOException {
        responseFormatterToCsv.fileWriter = mock(fileWriter.getClass());
        when(responseFormatterToCsv.fileWriter.append(anyString())).thenThrow(IOException.class);
        when(map.get(anyString())).thenReturn("dummy");
        assertThrows(RuntimeException.class,()-> responseFormatterToCsv.appendToCsvFile(map,stringSet));
        vertxTestContext.completeNow();
    }
}
