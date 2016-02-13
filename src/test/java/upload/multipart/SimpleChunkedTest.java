package upload.multipart;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultManagedHttpClientConnection;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.io.ChunkedOutputStream;
import org.apache.http.io.SessionOutputBuffer;
 


import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;
 
/**
 * Created by arul on 12/19/13.
 */
public class SimpleChunkedTest {
 
  public static class CustomManagedHttpClientConnection extends DefaultManagedHttpClientConnection {
    private final int chunkSize;
 
    public CustomManagedHttpClientConnection(final String id, final int buffersize, final int chunkSize) {
      super(id, buffersize);
      this.chunkSize = chunkSize;
    }
 
    @Override
    protected OutputStream createOutputStream(long len, SessionOutputBuffer outbuffer) {
      if (len == ContentLengthStrategy.CHUNKED) {
        return new ChunkedOutputStream(chunkSize, outbuffer);
      }
      return super.createOutputStream(len, outbuffer);
    }
  }
 
  public static class CustomManagedHttpClientConnectionFactory extends ManagedHttpClientConnectionFactory {
 
    private static final AtomicLong COUNTER = new AtomicLong();
    private final int chunkSize;
 
    public CustomManagedHttpClientConnectionFactory(int chunkSize) {
      this.chunkSize = chunkSize;
    }
 
    @Override
    public ManagedHttpClientConnection create(HttpRoute route, ConnectionConfig config) {
      final String id = "http-outgoing-" + Long.toString(COUNTER.getAndIncrement());
      return new CustomManagedHttpClientConnection(id, config.getBufferSize(), chunkSize);
    }
  }
 
  public static void main(String[] args) throws Exception {
    int chunkSize = 256;
    HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory = new CustomManagedHttpClientConnectionFactory(chunkSize);
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(connFactory);
    CloseableHttpClient client = HttpClients.createMinimal(connManager);
    HttpPost httpPost = new HttpPost("http://localhost:8080/defileserver-web/DumpServlet");
    
    InputStreamEntity reqEntity = new InputStreamEntity (
            new FileInputStream(new File("C:\\temp\\cubparts.jpg")), -1, ContentType.APPLICATION_OCTET_STREAM);
    reqEntity.setChunked(true);
    
    httpPost.setEntity(reqEntity);
    client.execute(httpPost);
  }
}

