package websockets;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.tyrus.client.ClientManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ElizaServerTest {

    @Value("${local.server.port}")
    private int port;
    private String URL;
    private String ELIZA_URL;

    private static final Logger LOGGER = Grizzly.logger(ElizaServerTest.class);


    @Before
    public void setup() {
        URL = "ws://localhost:" + port;
        ELIZA_URL = URL + "/eliza";
    }

	@Test(timeout = 5000)
	public void onOpen() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(3);
		List<String> list = new ArrayList<>();
		ClientEndpointConfig configuration = ClientEndpointConfig.Builder.create().build();
		ClientManager client = ClientManager.createClient();
		Session session = client.connectToServer(new Endpoint() {

			@Override
			public void onOpen(Session session, EndpointConfig config) {
				session.addMessageHandler(new ElizaOnOpenMessageHandler(list, latch));
			}

		}, configuration, new URI(ELIZA_URL));
        session.getAsyncRemote().sendText("bye");
        latch.await();
		assertEquals(3, list.size());
		assertEquals("The doctor is in.", list.get(0));
	}

    @Test(timeout = 1000)
    public void onChat() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        List<String> list = new ArrayList<>();
        ClientEndpointConfig configuration = ClientEndpointConfig.Builder.create().build();
        ClientManager client = ClientManager.createClient();
        Session session = client.connectToServer(new ElizaEndpointToComplete(list, latch), configuration, new URI(ELIZA_URL));

        session.getAsyncRemote().sendText("because");
        latch.await();

        //We have the first 3 messages sent by Eliza and then the because response.
        assertEquals(4, list.size());
        assertEquals("Is that the real reason?", list.get(3));
    }

    private static class ElizaOnOpenMessageHandler implements MessageHandler.Whole<String> {

        private final List<String> list;
        private final CountDownLatch latch;

        ElizaOnOpenMessageHandler(List<String> list, CountDownLatch latch) {
            this.list = list;
            this.latch = latch;
        }

        @Override
        public void onMessage(String message) {
            LOGGER.info(format("Client received \"%s\"", message));
            list.add(message);
            latch.countDown();
        }
    }

    private static class ElizaEndpointToComplete extends Endpoint {

        private final List<String> list;
        private final CountDownLatch latch;

        ElizaEndpointToComplete(List<String> list, CountDownLatch latch) {
            this.list = list;
            this.latch = latch;
        }

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            session.addMessageHandler(new ElizaMessageHandlerToComplete());
        }

        private class ElizaMessageHandlerToComplete implements MessageHandler.Whole<String> {

            @Override
            public void onMessage(String message) {
                list.add(message);
                latch.countDown();
            }
        }
    }
}
