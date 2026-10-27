package com.sprint.mission.discodeit.service.file;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;

import java.util.List;
import java.util.UUID;

/** Message 규칙과 연관 데이터 확인을 수행하고 영속화는 MessageRepository에 맡깁니다. */
public class FileMessageService implements MessageService {

    // 파일 저장과 조회는 MessageRepository에 맡깁니다.
    private final MessageRepository messageRepository;

    // 메시지 생성 시 작성자와 채널의 존재 여부를 확인하기 위한 서비스입니다.
    private final UserService userService;
    private final ChannelService channelService;

    public FileMessageService(
            MessageRepository messageRepository,
            UserService userService,
            ChannelService channelService
    ) {
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.channelService = channelService;
    }

    @Override
    public void create(Message message) {
        // 메시지와 ID를 검사한 후 작성자·채널이 존재하는지 확인하고 저장합니다.
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null.");
        }

        if (messageRepository.existsById(message.getId())) {
            throw new IllegalArgumentException(
                    "이미 등록된 ID입니다: " + message.getId()
            );
        }

        // 각 서비스는 ID가 존재하지 않으면 예외를 던지므로 이를 통해 참조를 검증합니다.
        userService.findById(message.getUserId());
        channelService.findById(message.getChannelId());

        messageRepository.save(message);
    }

    @Override
    public Message findById(UUID id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Not Found Message id: " + id
                ));
    }

    @Override
    public List<Message> findAll() {
        return messageRepository.findAll();
    }

    @Override
    public void update(UUID id, String content) {
        // 기존 메시지를 조회해 내용만 바꾸고 같은 ID로 Repository에 다시 저장합니다.
        Message existingMessage = findById(id);
        existingMessage.update(content);
        messageRepository.save(existingMessage);
    }

    @Override
    public void delete(UUID id) {
        // Repository의 삭제 결과가 false면 삭제 대상이 없다는 뜻입니다.
        if (!messageRepository.deleteById(id)) {
            throw new IllegalArgumentException("Message not found: " + id);
        }
    }
}
