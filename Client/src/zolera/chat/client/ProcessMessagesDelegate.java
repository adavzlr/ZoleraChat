package zolera.chat.client;

import zolera.chat.infrastructure.*;

public interface ProcessMessagesDelegate {
	public void process(ChatMessage[] batch);
}
