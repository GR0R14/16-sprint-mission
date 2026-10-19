package com.sprint.mission.discodeit.service.file;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileMessageService implements MessageService {

    // 메시지별 .ser 파일이 저장될 디렉터리입니다.
    private final Path directory;

    // Message 생성 때 작성자와 채널의 존재 여부를 확인하기 위해 필요합니다.
    private final UserService userService;
    private final ChannelService channelService;

    // 저장 위치와 연관 서비스를 전달받습니다.
    public FileMessageService(
            UserService userService,
            ChannelService channelService
    ) {
        this(
                userService,
                channelService,
                Paths.get(System.getProperty("user.dir"), "data", "messages")
        );
    }

    // 경로도 지정할 수 있는 생성자입니다.
    public FileMessageService(
            UserService userService,
            ChannelService channelService,
            Path directory
    ) {
        this.userService = userService;
        this.channelService = channelService;
        this.directory = directory;

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "메시지 저장 디렉터리를 만들 수 없습니다: " + directory, e
            );
        }
    }

    @Override
    public void create(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null.");
        }

        // 같은 ID의 메시지 파일이 이미 있는지 확인합니다.
        Path file = fileFor(message.getId());
        if (Files.exists(file)) {
            throw new IllegalArgumentException(
                    "이미 등록된 ID입니다: " + message.getId()
            );
        }

        // 작성자 ID가 실제 등록된 사용자인지 확인합니다.
        // 사용자가 없으면 UserService가 예외를 던집니다.
        userService.findById(message.getUserId());

        // 채널 ID가 실제 등록된 채널인지 확인합니다.
        // 채널이 없으면 ChannelService가 예외를 던집니다.
        channelService.findById(message.getChannelId());

        // 검증을 통과한 메시지를 파일로 저장합니다.
        write(file, message);
    }

    @Override
    public Message findById(UUID id) {
        Path file = fileFor(id);

        if (!Files.exists(file)) {
            throw new IllegalArgumentException(
                    "Not Found Message id: " + id
            );
        }

        return read(file);
    }

    @Override
    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();

        try (var files = Files.list(directory)) {
            files.filter(path ->
                            path.getFileName().toString().endsWith(".ser"))
                    .forEach(path -> messages.add(read(path)));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "메시지 파일 목록을 읽을 수 없습니다: " + directory, e
            );
        }

        return List.copyOf(messages);
    }

    @Override
    public void update(UUID id, String content) {
        // 먼저 수정할 메시지가 존재하는지 확인합니다.
        Message existingMessage = findById(id);

        // 기존 객체의 내용을 수정하고 updatedAt도 갱신합니다.
        existingMessage.update(content);

        // 수정된 메시지를 같은 ID 파일에 덮어씁니다.
        write(fileFor(id), existingMessage);
    }

    @Override
    public void delete(UUID id) {
        Path file = fileFor(id);

        try {
            if (!Files.deleteIfExists(file)) {
                throw new IllegalArgumentException(
                        "Message not found: " + id
                );
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "메시지 파일을 삭제할 수 없습니다: " + file, e
            );
        }
    }

    private Path fileFor(UUID id) {
        return directory.resolve(id + ".ser");
    }

    private void write(Path file, Message message) {
        try (ObjectOutputStream out =
                     new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(message);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "메시지 파일을 저장할 수 없습니다: " + file, e
            );
        }
    }

    private Message read(Path file) {
        try (ObjectInputStream in =
                     new ObjectInputStream(Files.newInputStream(file))) {
            return (Message) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(
                    "메시지 파일을 읽을 수 없습니다: " + file, e
            );
        }
    }
}