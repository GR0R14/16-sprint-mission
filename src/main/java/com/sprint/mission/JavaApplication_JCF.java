package com.sprint.mission;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.ChannelParticipationService;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.jcf.JCFChannelParticipationService;
import com.sprint.mission.discodeit.service.jcf.JCFChannelService;
import com.sprint.mission.discodeit.service.jcf.JCFMessageService;
import com.sprint.mission.discodeit.service.jcf.JCFUserService;

import java.util.UUID;

public class JavaApplication_JCF {

    public static void main(String[] args) {

        System.out.println("\n========== 사용자(User) ==========");

        UserService userService = new JCFUserService();
        User user = new User(
                "tester",
                "tester@example.com",
                "password123"
        );
        userService.create(user);

        System.out.println("사용자 생성: " + userService.findById(user.getId()).getUsername());
        System.out.println("등록된 사용자 수: " + userService.findAll().size());

        System.out.println("\n[이메일 형식 검증]");
        ServiceValidationMethods.printInvalidEmailCreateRejected(
                userService,
                new User("null email", null, "password123"),
                "null 이메일"
        );

        ServiceValidationMethods.printInvalidEmailCreateRejected(
                userService,
                new User("blank email", "   ", "password123"),
                "공백 이메일"
        );

        ServiceValidationMethods.printInvalidEmailCreateRejected(
                userService,
                new User("invalid email", "not-an-email", "password123"),
                "잘못된 형식 이메일"
        );

        System.out.println("\n[사용자 수정]");
        User updatedUser = new User(
                "updated tester",
                "tester@example.com",
                "updatedPassword123"
        );
        userService.update(user.getId(), updatedUser);
        System.out.println("수정된 사용자 이름: " + userService.findById(user.getId()).getUsername());

        System.out.println("\n[이메일 중복 검증]");
        User duplicateEmailUser = new User(
                "another tester",
                "tester@example.com", // 기존 user와 같은 이메일
                "password456"
        );
        ServiceValidationMethods.verifyDuplicateEmailRejected(userService, duplicateEmailUser);

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
        ServiceValidationMethods.verifyDuplicateEmailUpdateRejected(
                userService,
                user.getId(),
                user.getEmail(),
                duplicateEmailUpdate
        );

        System.out.println("\n========== 채널(Channel) ==========");

        ChannelService channelService = new JCFChannelService();
        Channel channel = new Channel(
                "notice channel",
                "Notice uploading Channel"
        );
        channelService.create(channel);

        System.out.println("채널 생성: " + channelService.findById(channel.getId()).getName());
        System.out.println("등록된 채널 수: " + channelService.findAll().size());

        System.out.println("\n[채널 수정]");
        channelService.update(
                channel.getId(),
                "updated notice channel",
                "Updated notice uploading Channel"
        );
        System.out.println("수정된 채널 이름: " + channelService.findById(channel.getId()).getName());

        System.out.println("\n========== 채널 참여 ==========");

        ChannelParticipationService participationService =
                new JCFChannelParticipationService(userService, channelService);

        participationService.joinChannel(user.getId(), channel.getId());
        participationService.joinChannel(secondUser.getId(), channel.getId());
        boolean joined = participationService
                .findParticipantIds(channel.getId())
                .contains(user.getId());

        System.out.println("채널 참여: " + (joined ? "성공" : "실패"));

        participationService.leaveChannel(user.getId(), channel.getId());
        boolean stillJoined = participationService
                .findParticipantIds(channel.getId())
                .contains(user.getId());

        System.out.println("채널 탈퇴: " + (!stillJoined ? "성공" : "실패"));

        // User 삭제 시 참여 관계 정리도 확인할 수 있도록 다시 참여
        participationService.joinChannel(user.getId(), channel.getId());

        System.out.println("\n========== 메시지(Message) ==========");

        MessageService messageService = new JCFMessageService(
                userService,
                channelService
        );

        Message message = new Message(
                "first message",
                user.getId(),
                channel.getId()
        );

        messageService.create(message);
        System.out.println("메시지 생성: 완료");

        UUID messageId = message.getId();

        System.out.println("메시지 내용: " + messageService.findById(messageId).getContent());
        System.out.println("등록된 메시지 수: " + messageService.findAll().size());

        System.out.println("\n[메시지 수정]");
        messageService.update(messageId, "updated meessage");
        System.out.println("수정된 메시지 내용: " + messageService.findById(messageId).getContent());

        System.out.println("\n[연관 데이터 검증 실패 사례]");

        Message messageWithMissingUser = new Message(
                "message with missing user",
                UUID.randomUUID(),
                channel.getId()
        );

        ServiceValidationMethods.printMessageCreationRejection(
                messageService,
                messageWithMissingUser,
                "잘못된 사용자 ID"
        );

        Message messageWithMissingChannel = new Message(
                "message with missing channel",
                user.getId(),
                UUID.randomUUID()
        );

        ServiceValidationMethods.printMessageCreationRejection(
                messageService,
                messageWithMissingChannel,
                "잘못된 채널 ID"
        );

        System.out.println("\n========== 삭제 및 연결 데이터 정리 ==========");

        messageService.delete(messageId);
        boolean messageStillExists = messageService.findAll().stream()
                .anyMatch(savedMessage -> savedMessage.getId().equals(message.getId()));
        System.out.println("메시지 삭제: " + (!messageStillExists ? "성공" : "실패"));

        Message messageForUserDelete = new Message(
                "message for user delete test",
                user.getId(),
                channel.getId()
        );
        messageService.create(messageForUserDelete);

        boolean userAndRelationsDeleted = deleteUserAndMessages(
                userService,
                messageService,
                participationService,
                user.getId()
        );

        System.out.println("User와 연결 데이터 삭제: "
                + (userAndRelationsDeleted ? "성공" : "실패"));

        Message messageForChannelDelete = new Message(
                "message for channel delete test",
                secondUser.getId(),
                channel.getId()
        );
        messageService.create(messageForChannelDelete);

        boolean channelAndRelationsDeleted = deleteChannelAndMessages(
                channelService,
                messageService,
                participationService,
                channel.getId()
        );

        System.out.println("Channel과 연결 데이터 삭제: "
                + (channelAndRelationsDeleted ? "성공" : "실패"));
    }

    private static boolean deleteUserAndMessages(
            UserService userService,
            MessageService messageService,
            ChannelParticipationService participationService,
            UUID userId
    ) {
        userService.findById(userId);

        messageService.findAll().stream()
                .filter(message -> message.getUserId().equals(userId))
                .map(Message::getId)
                .toList()
                .forEach(messageService::delete);

        // User 삭제 전에 참여 관계를 제거
        participationService.removeUserFromAllChannels(userId);
        boolean stillParticipates =
                !participationService.findChannelIds(userId).isEmpty();

        userService.delete(userId);

        boolean userStillExists = userService.findAll().stream()
                .anyMatch(user -> user.getId().equals(userId));
        boolean userMessageStillExists = messageService.findAll().stream()
                .anyMatch(message -> message.getUserId().equals(userId));

        return !userStillExists
                && !userMessageStillExists
                && !stillParticipates;
    }

    private static boolean deleteChannelAndMessages(
            ChannelService channelService,
            MessageService messageService,
            ChannelParticipationService participationService,
            UUID channelId
    ) {
        channelService.findById(channelId);

        messageService.findAll().stream()
                .filter(message -> message.getChannelId().equals(channelId))
                .map(Message::getId)
                .toList()
                .forEach(messageService::delete);

        // Channel 삭제 전에 모든 참여 관계를 제거
        participationService.removeAllParticipants(channelId);
        boolean stillHasParticipants =
                !participationService.findParticipantIds(channelId).isEmpty();

        channelService.delete(channelId);

        boolean channelStillExists = channelService.findAll().stream()
                .anyMatch(channel -> channel.getId().equals(channelId));
        boolean channelMessageStillExists = messageService.findAll().stream()
                .anyMatch(message -> message.getChannelId().equals(channelId));

        return !channelStillExists
                && !channelMessageStillExists
                && !stillHasParticipants;
    }
}
