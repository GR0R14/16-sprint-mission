package com.sprint.mission;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.file.FileChannelRepository;
import com.sprint.mission.discodeit.repository.file.FileMessageRepository;
import com.sprint.mission.discodeit.repository.file.FileUserRepository;
import com.sprint.mission.discodeit.repository.jcf.JCFChannelRepository;
import com.sprint.mission.discodeit.repository.jcf.JCFMessageRepository;
import com.sprint.mission.discodeit.repository.jcf.JCFUserRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.basic.BasicChannelService;
import com.sprint.mission.discodeit.service.basic.BasicMessageService;
import com.sprint.mission.discodeit.service.basic.BasicUserService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import com.sprint.mission.discodeit.dto.UserCreateRequest;
import com.sprint.mission.discodeit.dto.UserDto;
import com.sprint.mission.discodeit.repository.file.FileBinaryContentRepository;
import com.sprint.mission.discodeit.repository.file.FileUserStatusRepository;

public class JavaApplication {

    static UserDto setupUser(UserService userService) {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String username = "woody-" + runId;
        String email = "woody-" + runId + "@codeit.com";

        UserCreateRequest request = new UserCreateRequest(
                username,
                email,
                "woody1234",
                null
        );

        return userService.create(request);
    }

    static Channel setupChannel(ChannelService channelService) {
        return channelService.create(ChannelType.PUBLIC, "공지", "공지 채널입니다.");
    }

    static void messageCreateTest(
            MessageService messageService,
            Channel channel,
            UserDto author
    ) {
        Message message = messageService.create(
                "안녕하세요.", channel.getId(), author.id()
        );
        System.out.println("메시지 생성: " + message.getId());
    }

    public static void main(String[] args) {
        System.out.println("========== Basic 서비스 + JCF Repository ==========");
        UserRepository jcfUserRepository = new JCFUserRepository();
        ChannelRepository jcfChannelRepository = new JCFChannelRepository();
        MessageRepository jcfMessageRepository = new JCFMessageRepository();

        runScenario(
                new BasicUserService(
                        jcfUserRepository,
                        new FileBinaryContentRepository(),
                        new FileUserStatusRepository()
                ),
                new BasicChannelService(jcfChannelRepository),
                new BasicMessageService(
                        jcfMessageRepository, jcfUserRepository, jcfChannelRepository
                )
        );

        System.out.println("\n========== Basic 서비스 + File Repository ==========");
        Path directory = Paths.get(
                System.getProperty("user.dir"), "data", "basic-demo"
        );
        UserRepository fileUserRepository = new FileUserRepository(
                directory.resolve("users")
        );
        ChannelRepository fileChannelRepository = new FileChannelRepository(
                directory.resolve("channels")
        );
        MessageRepository fileMessageRepository = new FileMessageRepository(
                directory.resolve("messages")
        );

        runScenario(
                new BasicUserService(
                        fileUserRepository,
                        new FileBinaryContentRepository(directory.resolve("binary-contents")),
                        new FileUserStatusRepository(directory.resolve("user-statuses"))
                ),
                new BasicChannelService(fileChannelRepository),
                new BasicMessageService(
                        fileMessageRepository, fileUserRepository, fileChannelRepository
                )
        );
    }

    private static void runScenario(
            UserService userService,
            ChannelService channelService,
            MessageService messageService
    ) {
        System.out.println("실행 전 사용자 수: " + userService.findAll().size());
        System.out.println("실행 전 채널 수: " + channelService.findAll().size());
        System.out.println("실행 전 메시지 수: " + messageService.findAll().size());

        UserDto user = setupUser(userService);
        Channel channel = setupChannel(channelService);
        messageCreateTest(messageService, channel, user);

        System.out.println("실행 후 사용자 수: " + userService.findAll().size());
        System.out.println("실행 후 채널 수: " + channelService.findAll().size());
        System.out.println("실행 후 메시지 수: " + messageService.findAll().size());
    }
}
