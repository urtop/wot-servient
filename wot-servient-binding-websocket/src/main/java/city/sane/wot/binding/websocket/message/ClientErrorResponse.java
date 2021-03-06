package city.sane.wot.binding.websocket.message;

import city.sane.wot.content.Content;

public class ClientErrorResponse extends AbstractServerMessage {
    private final String reason;

    private ClientErrorResponse() {
        this.reason = null;
    }

    public ClientErrorResponse(AbstractClientMessage message, String reason) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public Content toContent() {
        return null;
    }
}
