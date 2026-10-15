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

        // null, 공백, 잘못된 형식 이메일 검증
        printInvalidEmailCreateRejected(
                userService,
                new User("null email", null, "password123"),
                "null 이메일"
        );

        printInvalidEmailCreateRejected(
                userService,
                new User("blank email", "   ", "password123"),
                "공백 이메일"
        );

        printInvalidEmailCreateRejected(
                userService,
                new User("invalid email", "not-an-email", "password123"),
                "잘못된 형식 이메일"
        );

        // update
        User updatedUser = new User(
                "updated tester",
                "tester@example.com",
                "updatedPassword123"
        );
        userService.update(user.getId(), updatedUser);
        System.out.println("User update: " + userService.findById(user.getId()).getUsername());

        // 이메일 생성 중복 검증
        User duplicateEmailUser = new User(
                "another tester",
                "tester@example.com", // 기존 user와 같은 이메일
                "password456"
        );
        verifyDuplicateEmailRejected(userService, duplicateEmailUser);

        // 이메일 수정 중복 검증
        User secondUser = new User(
                "second tester",
                "second@example.com",
                "password456"
        );
        userService.create(secondUser);

        User duplicateEmailUpdate = new User(
                "updated tester",
                secondUser.getEmail(), // 다른 사용자가 이미 쓰는 이메일
                "updatedPassword123"
        );
        verifyDuplicateEmailUpdateRejected(
                userService,
                user.getId(),
                user.getEmail(),
                duplicateEmailUpdate
        );

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

        // User 연쇄 삭제 확인용 메시지
        Message messageForUserDelete = new Message(
                "message for user delete test",
                user.getId(),
                channel.getId()
        );
        messageService.create(messageForUserDelete);

        boolean userAndMessagesDeleted =
                deleteUserAndMessages(userService, messageService, user.getId());

        System.out.println("User 및 연결 메시지 삭제: "
                + (userAndMessagesDeleted ? "성공" : "실패"));

        // Channel 연쇄 삭제 확인용 메시지
        Message messageForChannelDelete = new Message(
                "message for channel delete test",
                secondUser.getId(),
                channel.getId()
        );
        messageService.create(messageForChannelDelete);

        boolean channelAndMessagesDeleted =
                deleteChannelAndMessages(channelService, messageService, channel.getId());

        System.out.println("Channel 및 연결 메시지 삭제: "
                + (channelAndMessagesDeleted ? "성공" : "실패"));

    }

    // static: 객체안만들어도 호출하기
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

    private static void printInvalidEmailCreateRejected(
            UserService userService,
            User user,
            String testName
    ) {
        boolean rejected = false;

        try {
            userService.create(user);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }

        boolean wasSaved = userService.findAll().stream()
                .anyMatch(savedUser ->
                        savedUser.getId().equals(user.getId()));

        System.out.println(testName + " 생성 검증: "
                + (rejected && !wasSaved
                ? "예외 발생, 저장되지 않음"
                : "검증 실패"));
    }

    private static void verifyDuplicateEmailRejected(
            UserService userService,
            User user
    ) {
        boolean rejected = false;

        try {
            userService.create(user);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }

        boolean wasSaved = userService.findAll().stream()
                .anyMatch(savedUser -> savedUser.getId().equals((user.getId())));

        System.out.println("중복 이메일 생성 거부: "
                + (rejected && !wasSaved));
    }

    private static void verifyDuplicateEmailUpdateRejected(
            UserService userService,
            UUID userId,
            String originalEmail,
            User updatedUser
    ) {
        boolean rejected = false;

        try {
            userService.update(userId, updatedUser);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }

        boolean emailUnchanged = userService.findById(userId)
                .getEmail()
                .equals(originalEmail);

        System.out.println("중복 이메일 수정 거부: "
                + (rejected && emailUnchanged));
    }

    private static boolean deleteUserAndMessages(
            UserService userService,
            MessageService messageService,
            UUID userId
    ) {
        userService.findById(userId);

        messageService.findAll().stream()
                .filter(message -> message.getUserId().equals(userId))
                .map(Message::getId)
                .toList()
                .forEach(messageService::delete);

        userService.delete(userId);

        boolean userStillExists = userService.findAll().stream()
                .anyMatch(user -> user.getId().equals(userId));
        boolean userMessageStillExists = messageService.findAll().stream()
                .anyMatch(message -> message.getUserId().equals(userId));

        return !userStillExists && !userMessageStillExists;
    }

    private static boolean deleteChannelAndMessages(
            ChannelService channelService,
            MessageService messageService,
            UUID channelId
    ) {
        channelService.findById(channelId);

        messageService.findAll().stream()
                .filter(message -> message.getChannelId().equals(channelId))
                .map(Message::getId)
                .toList()
                .forEach(messageService::delete);

        channelService.delete(channelId);

        boolean channelStillExists = channelService.findAll().stream()
                .anyMatch(channel -> channel.getId().equals(channelId));
        boolean channelMessageStillExists = messageService.findAll().stream()
                .anyMatch(message -> message.getChannelId().equals(channelId));

        return !channelStillExists && !channelMessageStillExists;
    }
}
