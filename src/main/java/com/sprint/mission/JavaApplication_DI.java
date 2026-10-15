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

        // delete
        messageService.delete(messageId);
        System.out.println("message 삭제 완료");
        System.out.println("message count: " + messageService.findAll().size());

    }
}
