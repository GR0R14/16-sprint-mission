package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;

import java.util.List;
import java.util.UUID;

public interface ChannelService {

    void create(Channel channel);

    default Channel create(ChannelType type, String name, String description) {
        Channel channel = new Channel(type, name, description);
        create(channel);
        return channel;
    }

    Channel findById(UUID uuid);

    List<Channel> findAll();

    void update(UUID id, String name, String description);

    void delete(UUID id);

}
