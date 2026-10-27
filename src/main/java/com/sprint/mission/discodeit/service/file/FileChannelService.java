package com.sprint.mission.discodeit.service.file;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.service.ChannelService;

import java.util.List;
import java.util.UUID;

/** Channel 업무 흐름을 처리하고 저장·조회는 ChannelRepository에 위임합니다. */
public class FileChannelService implements ChannelService {

    // 채널 관련 비즈니스 흐름을 담당하고,
    // 파일 저장 작업은 주입받은 FileChannelRepository에 위임합니다.
    private final ChannelRepository channelRepository;

    public FileChannelService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    public void create(Channel channel) {
        // null과 중복 ID를 검사한 뒤에만 Repository에 저장합니다.
        if (channel == null) {
            throw new IllegalArgumentException("Channel cannot be null.");
        }

        if (channelRepository.existsById(channel.getId())) {
            throw new IllegalArgumentException(
                    "이미 등록된 ID입니다: " + channel.getId()
            );
        }

        channelRepository.save(channel);
    }

    @Override
    public Channel findById(UUID id) {
        return channelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Not Found channel id: " + id
                ));
    }

    @Override
    public List<Channel> findAll() {
        return channelRepository.findAll();
    }

    @Override
    public void update(UUID id, Channel channel) {
        // 저장된 객체를 찾아 내용을 바꾸고 Repository.save로 변경 사항을 반영합니다.
        Channel existingChannel = findById(id);

        existingChannel.update(
                channel.getName(),
                channel.getDescription()
        );

        channelRepository.save(existingChannel);
    }

    @Override
    public void delete(UUID id) {
        // 삭제 여부를 확인해 대상이 없으면 Service 차원의 예외를 던집니다.
        if (!channelRepository.deleteById(id)) {
            throw new IllegalArgumentException("Not Found channel id: " + id);
        }
    }
}
