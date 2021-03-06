package sc.server.network;

import sc.networking.TcpNetwork;
import sc.networking.clients.XStreamClient;
import sc.protocol.responses.ProtocolMessage;

import java.io.IOException;
import java.net.Socket;

public class TestTcpClient extends XStreamClient {
  public TestTcpClient(Socket socket) throws IOException {
    super(new TcpNetwork(socket));
  }

  @Override
  protected void onObject(ProtocolMessage o) {
    // ignore it
    // LoggerFactory.getLogger(this.getClass()).debug("Received: {}", o);
  }

}
