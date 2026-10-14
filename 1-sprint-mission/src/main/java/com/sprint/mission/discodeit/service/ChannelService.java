package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.entity.Channel;

import java.util.List;
import java.util.UUID;

public interface ChannelService {

    // 채널 등록
    void create(Channel channel);

    // 채널 조회
    Channel findById(UUID uuid);

    // 모든 채널 조회
    List<Channel> findAll();

    // 채널 수정
    void update(UUID id, Channel channel);

    // 채널 삭제
    void delete(UUID id);

}
