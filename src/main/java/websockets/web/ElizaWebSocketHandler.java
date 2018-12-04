package websockets.web;

import org.glassfish.grizzly.Grizzly;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import websockets.service.Eliza;

import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElizaWebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = Grizzly.logger(ElizaWebSocketHandler.class);
    private Eliza eliza = new Eliza();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOGGER.info("Server Connected ... " + session.getId());
        session.sendMessage(new TextMessage("The doctor is in."));
        session.sendMessage(new TextMessage("What's on your mind?"));
        session.sendMessage(new TextMessage("---"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        LOGGER.info(String.format("Session %s closed because of %s", session.getId(), status.getReason()));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        LOGGER.log(Level.SEVERE,
                String.format("Session %s closed because of %s", session.getId(), exception.getClass().getName()),
                exception);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        LOGGER.info("Server Message ... " + session.getId());
        Scanner currentLine = new Scanner(message.getPayload().toLowerCase());
        if (currentLine.findInLine("bye") == null) {
            LOGGER.info("Server recieved \"" + message + "\"");
            session.sendMessage(new TextMessage(eliza.respond(currentLine)));
            session.sendMessage(new TextMessage("---"));
        } else {
            session.close(new CloseStatus(CloseStatus.NORMAL.getCode(), "Alright then, goodbye!"));
        }
    }

}
