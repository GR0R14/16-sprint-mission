package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;

import javax.swing.text.ChangedCharSetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JCFMessageService implements MessageService {

    private final Map<UUID, Message> data;
    private final UserService userService;
    private final ChannelService channelService;

    public JCFMessageService(
            UserService userService,
            ChannelService channelService
    ) {
        this.data = new HashMap<>();
        this.userService = userService;
        this.channelService = channelService;
    }

    @Override
    public void create(Message message) {

        // User 조회
        User user = userService.findById(message.getUserId());
        if (user == null) {
            throw new IllegalArgumentException(
                    "User not found: " + message.getUserId()
            );
        }

        // channel 조회
        Channel channel = channelService.findById(message.getChannelId());
        if (channel == null) {
            throw new IllegalArgumentException(
                    "Channel not found: " + message.getChannelId()
            );
        }

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
                    "Not found Message: " + id
            );
        }

        existingMessage.update(message.getContent());
    }

    @Override
    public void delete(UUID id) {
        data.remove(id);
    }
}
