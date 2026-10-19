package com.sprint.mission.discodeit.service.file;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.service.ChannelService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileChannelService implements ChannelService {

    // 채널별 .ser 파일을 저장할 디렉터리입니다.
    private final Path directory;

    // 기본 저장 위치는 실행 디렉터리 아래의 data/channels입니다.
    public FileChannelService() {
        this(Paths.get(System.getProperty("user.dir"), "data", "channels"));
    }

    // 경로를 직접 지정하는 생성자입니다.
    // 테스트나 다른 실행 환경에서 저장 위치를 바꿀 때 사용할 수 있습니다.
    public FileChannelService(Path directory) {
        this.directory = directory;

        try {
            // 저장 디렉터리가 없으면 필요한 상위 디렉터리까지 만듭니다.
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "채널 저장 디렉터리를 만들 수 없습니다: " + directory, e
            );
        }
    }

    @Override
    public void create(Channel channel) {
        // null은 채널로 저장할 수 없습니다.
        if (channel == null) {
            throw new IllegalArgumentException("Channel cannot be null.");
        }

        // 채널 ID에 대응하는 파일 경로를 만듭니다.
        Path file = fileFor(channel.getId());

        // 같은 ID 파일이 있으면 중복 등록으로 처리합니다.
        if (Files.exists(file)) {
            throw new IllegalArgumentException(
                    "이미 등록된 ID입니다: " + channel.getId()
            );
        }

        // 채널 객체를 직렬화해 파일에 저장합니다.
        write(file, channel);
    }

    @Override
    public Channel findById(UUID id) {
        Path file = fileFor(id);

        // ID에 해당하는 파일이 없으면 채널이 없는 것으로 처리합니다.
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Not Found channel id: " + id);
        }

        // 파일을 읽어 Channel 객체로 복원합니다.
        return read(file);
    }

    @Override
    public List<Channel> findAll() {
        // 복원한 채널을 담을 목록입니다.
        List<Channel> channels = new ArrayList<>();

        // 디렉터리에 있는 파일 이름을 조회합니다.
        try (var files = Files.list(directory)) {
            // .ser 파일만 골라 각 파일을 Channel 객체로 복원합니다.
            files.filter(path ->
                            path.getFileName().toString().endsWith(".ser"))
                    .forEach(path -> channels.add(read(path)));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "채널 파일 목록을 읽을 수 없습니다: " + directory, e
            );
        }

        // 내부 목록을 외부에서 수정하지 못하도록 복사본을 반환합니다.
        return List.copyOf(channels);
    }

    @Override
    public void update(UUID id, Channel channel) {
        // 먼저 기존 채널을 조회합니다.
        // 채널 파일이 없으면 findById에서 예외가 발생합니다.
        Channel existingChannel = findById(id);

        // JCFChannelService와 같이 기존 객체의 필드를 수정합니다.
        existingChannel.update(
                channel.getName(),
                channel.getDescription()
        );

        // 수정된 채널을 같은 ID의 파일에 다시 저장합니다.
        write(fileFor(id), existingChannel);
    }

    @Override
    public void delete(UUID id) {
        Path file = fileFor(id);

        try {
            // 파일을 삭제합니다.
            // 삭제할 파일이 없으면 사용자에게 채널이 없다고 알립니다.
            if (!Files.deleteIfExists(file)) {
                throw new IllegalArgumentException(
                        "Not Found channel id: " + id
                );
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "채널 파일을 삭제할 수 없습니다: " + file, e
            );
        }
    }

    // UUID를 파일 이름으로 사용해 채널 파일 경로를 만듭니다.
    private Path fileFor(UUID id) {
        return directory.resolve(id + ".ser");
    }

    // 객체를 직렬화해 파일에 씁니다.
    private void write(Path file, Channel channel) {
        try (ObjectOutputStream out =
                     new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(channel);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "채널 파일을 저장할 수 없습니다: " + file, e
            );
        }
    }

    // 파일에서 객체를 읽어 Channel로 복원합니다.
    private Channel read(Path file) {
        try (ObjectInputStream in =
                     new ObjectInputStream(Files.newInputStream(file))) {
            return (Channel) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(
                    "채널 파일을 읽을 수 없습니다: " + file, e
            );
        }
    }
}