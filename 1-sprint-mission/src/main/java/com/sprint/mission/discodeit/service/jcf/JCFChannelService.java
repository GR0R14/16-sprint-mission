package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.service.ChannelService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JCFChannelService implements ChannelService {

    // Map<K,V>
    private final Map<UUID, Channel> data;

    // 생성자
    public JCFChannelService() {
        this.data = new HashMap<>();
    }

    // 채널 생성
    @Override
    public void create(Channel channel) {
        data.put(channel.getId(), channel);
    }

    // 채널 조회
    @Override
    public Channel findById(UUID id) {
        return data.get(id);
    }

    // 모든 채널 조회
    @Override
    public List<Channel> findAll() {
        return List.copyOf(data.values());
    }

    // 채널 수정
    @Override
    public void update(UUID id, Channel channel) {
        Channel existingChannel = data.get(id);

        if (existingChannel == null) {
            throw new IllegalArgumentException(
                    "Not existing channel: " + id
            );
        }

        existingChannel.update(
                channel.getName(),
                channel.getDescription()
        );
    }

    // 채널 삭제
    @Override
    public void delete(UUID id) {
        data.remove(id);
    }
}
