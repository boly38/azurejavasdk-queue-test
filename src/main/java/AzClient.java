import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.ws.rs.core.MediaType;

import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.servicebus.ServiceBusConfiguration;
import com.microsoft.windowsazure.services.servicebus.ServiceBusContract;
import com.microsoft.windowsazure.services.servicebus.ServiceBusService;
import com.microsoft.windowsazure.services.servicebus.models.BrokeredMessage;
import com.microsoft.windowsazure.services.servicebus.models.ReceiveMessageOptions;
import com.microsoft.windowsazure.services.servicebus.models.ReceiveQueueMessageResult;

public class AzClient {
	private static transient SecureRandom randomGenerator = new SecureRandom();
	private static final Integer POLL_TIMEOUT_SEC = 3;
	private static String queueName = null;

	private ServiceBusContract service = null;

	public AzClient() {
		queueName = getEnv("AC_QUEUENAME");
		String busDomainPrefix = getEnv("AC_BUSDOMAIN");
		String sasPolicyName = getEnv("AC_POLICYNAME");
		String sasPolicyKey = getEnv("AC_POLICYKEY");
		int pkeySize = sasPolicyKey != null ? sasPolicyKey.length() : 0;
		String azureBusDomain = ".servicebus.windows.net";
		log(String.format("configure azure service bus: domain:%s, policy:%s, pkeySize:%s, busDomain:%s",
				busDomainPrefix, sasPolicyName, pkeySize, azureBusDomain));
		Configuration config = ServiceBusConfiguration.configureWithSASAuthentication(busDomainPrefix, sasPolicyName,
				sasPolicyKey, azureBusDomain);
		service = ServiceBusService.create(config);
	}

	private String getEnv(String name) {
		String getenv = System.getenv(name);
		if (getenv == null) {
			throw new IllegalStateException(String.format("environment variable '%s' is missing", name));
		}
		return getenv;
	}

	private static void log(String msg) {
		System.out.println(msg);
	}

	ReceiveMessageOptions _getReceiveMessageOptions() {
		ReceiveMessageOptions opts = ReceiveMessageOptions.DEFAULT;
		opts.setReceiveAndDelete();
		opts.setTimeout(POLL_TIMEOUT_SEC);
		return opts;
	}
	
    public static String convertStreamToString(InputStream is) {
        if (is == null) return null;
        StringBuilder sb = new StringBuilder(2048); // Define a size if you have an idea of it.
        char[] read = new char[128]; // Your buffer size.
        try (InputStreamReader ir = new InputStreamReader(is, StandardCharsets.UTF_8)) {
          for (int i; -1 != (i = ir.read(read)); sb.append(read, 0, i));
        } catch (Throwable t) {
        	log(String.format("non blocking exception while reading message : %s", t.getMessage()));
        }
        return sb.toString();
     }

    public String readMessage(BrokeredMessage msg) throws IOException {
        if (msg == null) {
            return null;
        }
        String msgBody = convertStreamToString(msg.getBody());
        int bodySize = msgBody != null ? msgBody.length() : 0;

        String messageId = msg.getMessageId();
        if (messageId == null && (msgBody == null || msgBody.isEmpty())) {
            log("."); //  >>> RECEIVED null message");
            return null;
        }
        log(String.format(" >>> RECEIVED #%s size:%db body:%s", messageId, bodySize, msgBody));
        return msgBody;
    }
    
	public String pollQueue() throws ServiceException {
		ReceiveMessageOptions opts = _getReceiveMessageOptions();
		ReceiveQueueMessageResult rMsg = service.receiveQueueMessage(queueName, opts);
		BrokeredMessage bMsg = rMsg != null ? rMsg.getValue() : null;
		try {
			String messageBody = readMessage(bMsg);
			return messageBody;
		} catch (IOException e) {
			String exMsg = e != null ? e.getMessage() : "(unknown)";
			throw new ServiceException("unable to read message : " + exMsg, e);
		}
	}

	public void shutdown() {
		log("shutdown azure bus client");
		service = null;
	}

	protected long getRandomMessageId() {
		long msgId = randomGenerator.nextLong() >>> 1;
		// long msgId = UUID.randomUUID().getMostSignificantBits();
		return msgId;
	}

	public void sendMsg(String sendMsgTxt) throws ServiceException {
		BrokeredMessage brokMsg = new BrokeredMessage(sendMsgTxt);
		brokMsg.setContentType(MediaType.APPLICATION_JSON);
		String messageId = "ID:" + getRandomMessageId();
		brokMsg.setMessageId(messageId);
		service.sendQueueMessage(queueName, brokMsg);
		log(String.format(" <<< SENT #%s size:%db body:%s", messageId, sendMsgTxt.length(), sendMsgTxt));
	}

}
