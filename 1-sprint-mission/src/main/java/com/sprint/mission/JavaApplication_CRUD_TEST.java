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

public class JavaApplication_CRUD_TEST {

    public static void main(String[] args) {

        // USERSERVICE CRUD TESTING.
        System.out.println("==============================");
        System.out.println("USER CRUD TESTING");
        System.out.println("==============================");

        UserService userService = new JCFUserService();

        User user = new User(
                "tester",
                "tester@example.com",
                "password123"
        );

        // [] 등록
        userService.create(user);
        UUID userId = user.getId();

        // [ ] 조회(단건, 다건)
        System.out.println("조회: " + userService.findById(userId).getUsername());
        System.out.println("사용자 수: " + userService.findAll().size());

        // [ ] 수정
        User updatedUser = new User(
                "tester-updated",
                "updated@example.com",
                "newpassword123"
        );

        userService.update(userId, updatedUser);

        // [ ] 수정된 데이터 조회
        System.out.println("update check: " + userService.findById(userId).getUsername());

        // [ ] 삭제
        userService.delete(userId);
        // [ ] 조회를 통해 삭제되었는지 확인
        User deletedUser = userService.findById(userId);
        System.out.println(
                deletedUser == null ? "user 삭제 완료" : "user 삭제 실패"
        );
        System.out.println("user count: " + userService.findAll().size());


        //
        // CHANNEL CRUD TESTING.
        System.out.println("==============================");
        System.out.println("CHANNEL CRUD TESTING.");
        System.out.println("==============================");

        ChannelService channelService = new JCFChannelService();

        Channel channel = new Channel(
                "notice channel",
                "Notice uploading Channel"
        );

        // create
        channelService.create(channel);
        UUID channelId = channel.getId();

        // Read
        System.out.println("channel read: " + channelService.findById(channelId).getName());
        System.out.println("channel count: " + channelService.findAll().size());

        Channel updatedChannel = new Channel(
                "Casual Channel",
                "Casual Chatting Channel"
        );
        channelService.update(channelId, updatedChannel);
        System.out.println("update channel check: " + channelService.findById(channelId).getName());

        // delete
        channelService.delete(channelId);
        Channel deletedChannel = channelService.findById(channelId);
        System.out.println(
                deletedChannel == null ? "channel 삭제 완료" : "channel 삭제 실패"
        );
        System.out.println("channel count: " + channelService.findAll().size());


        //
        // Message CRUD TESTING.
        System.out.println("==============================");
        System.out.println("MESSAGE CRUD TESTING.");
        System.out.println("==============================");

        MessageService messageService = new JCFMessageService(
                userService,
                channelService
        );

        Message message = new Message(
                "first message",
                userId,
                channelId
        );

        // create
        messageService.create(message);
        UUID messageId = message.getId();

        // read
        System.out.println("message: " + messageService.findById(messageId).getContent());

        // readAll
        System.out.println("message count: " + messageService.findAll().size());

        // update
        Message updatedMessage = new Message(
                "updated meessage",
                userId,
                channelId
        );
        messageService.update(messageId, updatedMessage);

        System.out.println("message update: " + messageService.findById(messageId).getContent());

        // delete
        messageService.delete(messageId);

        Message deletedMessage = messageService.findById(messageId);

        System.out.println(
                deletedMessage == null ? "message 삭제 완료" : "message 삭제 실패"
        );
        System.out.println("message count: " + messageService.findAll().size());

    }
}
