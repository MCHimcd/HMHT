package ww.wangnong.service;

/**
 * Purpose: 定义农业问答服务契约。
 * Responsibilities: 接收文本与可选图像上下文，返回统一的问答结果结构。
 * Flow: Controller 提交 message/image 参数 -> 实现类路由到 mock 或 agri chat 后端 -> 返回
 * ChatResult。
 * Dependencies: 各运行模式下的 ChatService 实现。
 * Input: 文本消息、图片 URI、图片 base64。
 * Output: 单条问答回复。
 */
public interface ChatService {

    record ChatResult(String reply) {
    }

    ChatResult chat(String message, String imageUri, String imageBase64);
}
