package city.sane.wot.binding.websocket.message;

import city.sane.wot.content.Content;

public class ServerErrorResponse extends AbstractServerMessage {

    private final String reason;

    public String getReason() {
        return reason;
    }

    private ServerErrorResponse() {
        this.reason = null;
    }

    public ServerErrorResponse(AbstractClientMessage message, String reason) {
        super(message);
        this.reason = reason;
    }

    @Override
    public Content toContent() {
        return null;
    }
}
