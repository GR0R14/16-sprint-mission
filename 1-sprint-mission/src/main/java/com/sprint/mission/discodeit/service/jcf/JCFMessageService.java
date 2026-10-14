package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.service.MessageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JCFMessageService implements MessageService {

    private final Map<UUID, Message> data;

    public JCFMessageService() {
        this.data = new HashMap<>();
    }

    @Override
    public void create(Message message) {
        data.put(message.getId(), message);
    }

    @Override
    public Message findById(UUID id) {
        return data.get(id);
    }

    @Override
    public List<Message> findAll() {
        return List.copyOf(data.values());
    }

    @Override
    public void update(UUID id, Message message) {
        Message existingMessage = data.get(id);

        if (existingMessage == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 메시지입니다: " + id
            );
        }

        existingMessage.update(message.getContent());
    }

    @Override
    public void delete(UUID id) {
        data.remove(id);
    }
}
