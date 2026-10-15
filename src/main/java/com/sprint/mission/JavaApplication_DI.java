package com.sprint.mission;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.jcf.JCFChannelService;
import com.sprint.mission.discodeit.service.jcf.JCFMessageService;
import com.sprint.mission.discodeit.service.jcf.JCFUserService;

import java.util.UUID;

public class JavaApplication_DI {

    public static void main(String[] args) {

        // [] USER 등록
        UserService userService = new JCFUserService();
        User user = new User(
                "tester",
                "tester@example.com",
                "password123"
        );
        userService.create(user);

        // [ ] 조회(단건, 다건)
        System.out.println("Create User: " + userService.findById(user.getId()).getUsername());
        System.out.println("User counting: " + userService.findAll().size());

        // update
        User updatedUser = new User(
                "updated tester",
                "tester@example.com",
                "updatedPassword123"
        );
        userService.update(user.getId(), updatedUser);
        System.out.println("User update: " + userService.findById(user.getId()).getUsername());

        //
        // [] CHANNEL 등록
        ChannelService channelService = new JCFChannelService();
        Channel channel = new Channel(
                "notice channel",
                "Notice uploading Channel"
        );
        channelService.create(channel);

        System.out.println("Create Channel: " + channelService.findById(channel.getId()).getName());
        System.out.println("Channel counting: " + channelService.findAll().size());

        // update
        Channel updatedChannel = new Channel(
                "updated notice channel",
                "Updated notice uploading Channel"
        );
        channelService.update(channel.getId(), updatedChannel);
        System.out.println("Channel update: " + channelService.findById(channel.getId()).getName());

        // MESSAGE_DI CRUD TEST
        // [] MESSAGE 등록
        MessageService messageService = new JCFMessageService(
                userService,
                channelService
        );

        Message message = new Message(
                "first message",
                user.getId(),
                channel.getId()
        );

        // create
        messageService.create(message);
        System.out.println("message 생성 완료");

        UUID messageId = message.getId();

        // read
        System.out.println("message: " + messageService.findById(messageId).getContent());
        System.out.println("message count: " + messageService.findAll().size());

        // update
        messageService.update(messageId, "updated meessage");
        System.out.println("message 수정 완료");
        System.out.println("message update: " + messageService.findById(messageId).getContent());

        // 잘못된 사용자 ID의 객체 생성
        Message messageWithMissingUser = new Message(
                "message with missing user",
                UUID.randomUUID(),
                channel.getId()
        );

        printMessageCreationRejection(
                messageService,
                messageWithMissingUser,
                "ID 검증"
        );

        Message messageWithMissingChannel = new Message(
                "message with missing channel",
                user.getId(),
                UUID.randomUUID()
        );

        printMessageCreationRejection(
                messageService,
                messageWithMissingChannel,
                "Channel 검증"
        );

        // delete: Message User, Channel 삭제
        messageService.delete(messageId);
        boolean messageStillExists = messageService.findAll().stream()
                .anyMatch(savedMessage -> savedMessage.getId().equals(message.getId()));
        System.out.println("Message 삭제: " + !messageStillExists);

        userService.delete(user.getId());
        boolean userStillExists = userService.findAll().stream()
                .anyMatch(savedUser -> savedUser.getId().equals(user.getId()));
        System.out.println("User 삭제: " + !userStillExists);

        channelService.delete(channel.getId());
        boolean channelStillExists = channelService.findAll().stream()
                .anyMatch(savedChannel -> savedChannel.getId().equals(channel.getId()));
        System.out.println("Channel 삭제: " + !channelStillExists);

    }

    private static void printMessageCreationRejection(
            MessageService messageService,
            Message message,
            String testName
    ) {
        boolean rejected = false;

        try {
            messageService.create(message);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }

        boolean wasSaved = messageService.findAll().stream()
                .anyMatch(savedMessage ->
                        savedMessage.getId().equals(message.getId()));

        boolean passed = rejected && !wasSaved;
        System.out.println(testName
                + " 검증 결과: 예외 발생=" + rejected
                + ", 메시지 저장 여부=" + wasSaved);
    }
}