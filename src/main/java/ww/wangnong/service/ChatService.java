package ww.wangnong.service;

public interface ChatService {

    record ChatResult(String reply) {}

    ChatResult chat(String message, String imageUri);
}
