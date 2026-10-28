package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** 채널 업무 흐름을 처리하고 영속화는 ChannelRepository에 맡깁니다. */
@Service
public class BasicChannelService implements ChannelService {

    private final ChannelRepository channelRepository;

    public BasicChannelService(ChannelRepository channelRepository) {
        if (channelRepository == null) {
            throw new IllegalArgumentException("채널 저장소는 null일 수 없습니다.");
        }
        this.channelRepository = channelRepository;
    }

    @Override
    public void create(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("채널은 null일 수 없습니다.");
        }
        if (channelRepository.existsById(channel.getId())) {
            throw new IllegalArgumentException("이미 등록된 채널 ID입니다: " + channel.getId());
        }

        channelRepository.save(channel);
    }

    @Override
    public Channel findById(UUID id) {
        return channelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("채널을 찾을 수 없습니다: " + id));
    }

    @Override
    public List<Channel> findAll() {
        return channelRepository.findAll();
    }

    @Override
    public void update(UUID id, String name, String description) {
        Channel existingChannel = findById(id);
        existingChannel.update(name, description);
        channelRepository.save(existingChannel);
    }

    @Override
    public void delete(UUID id) {
        if (!channelRepository.deleteById(id)) {
            throw new IllegalArgumentException("채널을 찾을 수 없습니다: " + id);
        }
    }
}
