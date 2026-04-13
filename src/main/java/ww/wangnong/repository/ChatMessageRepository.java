package ww.wangnong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ww.wangnong.entity.ChatMessage;

/**
 * Purpose: 提供聊天消息历史查询。
 * Responsibilities: 保存消息并按用户和时间正序读取会话历史。
 * Flow: Chat 相关服务写入消息；会话历史功能按用户查询。
 * Dependencies: ChatMessage 实体。
 * Output: 某用户的对话消息列表。
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(Long userId);
}
