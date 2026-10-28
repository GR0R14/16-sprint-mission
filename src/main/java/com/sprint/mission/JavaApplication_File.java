package com.sprint.mission;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.ChannelParticipationService;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.file.FileChannelService;
import com.sprint.mission.discodeit.service.file.FileMessageService;
import com.sprint.mission.discodeit.service.file.FileUserService;
import com.sprint.mission.discodeit.service.jcf.JCFChannelParticipationService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * File*Service를 실행해 파일 영속화를 확인하는 간단한 실행 예제입니다.
 * 채널 참여 관계만 기존 JCF 구현을 사용합니다.
 */
public class JavaApplication_File {

    public static void main(String[] args) {
        Path dataDirectory = Paths.get(
                System.getProperty("user.dir"), "data", "file-demo"
        );

        UserService userService = new FileUserService(dataDirectory.resolve("users"));
        ChannelService channelService = new FileChannelService(
                dataDirectory.resolve("channels")
        );

        // 이 구현체의 참여 관계는 JCF 메모리에 보관되므로 프로그램을 종료하면 사라집니다.
        ChannelParticipationService participationService =
                new JCFChannelParticipationService(userService, channelService);

        MessageService messageService = new FileMessageService(
                userService,
                channelService,
                dataDirectory.resolve("messages")
        );

        System.out.println("========== 기존 파일 데이터 ==========");
        System.out.println("저장된 사용자 수: " + userService.findAll().size());
        System.out.println("저장된 채널 수: " + channelService.findAll().size());
        System.out.println("저장된 메시지 수: " + messageService.findAll().size());

        // 이 값을 이메일에 넣어 재실행 때 기존 이메일과 중복되지 않게 합니다.
        String runId = UUID.randomUUID().toString().substring(0, 8);
        User user = new User(
                "file tester " + runId,
                "file-tester-" + runId + "@example.com",
                "password123"
        );
        userService.create(user);

        System.out.println("\n========== 사용자 파일 저장 확인 ==========");
        System.out.println("생성한 사용자: " + userService.findById(user.getId()).getUsername());

        Channel channel = new Channel(
                "file channel " + runId,
                "File persistence demo channel"
        );
        channelService.create(channel);

        System.out.println("\n========== 채널 파일 저장 확인 ==========");
        System.out.println("생성한 채널: " + channelService.findById(channel.getId()).getName());

        System.out.println("\n========== JCF 채널 참여 확인 ==========");
        participationService.joinChannel(user.getId(), channel.getId());
        boolean joined = participationService
                .findParticipantIds(channel.getId())
                .contains(user.getId());
        System.out.println("채널 참여: " + (joined ? "성공" : "실패"));

        Message message = new Message(
                "first file message",
                user.getId(),
                channel.getId()
        );
        messageService.create(message);

        System.out.println("\n========== 메시지 파일 저장 확인 ==========");
        System.out.println("생성한 메시지: " + messageService.findById(message.getId()).getContent());

        messageService.update(message.getId(), "updated file message");
        System.out.println("수정한 메시지: " + messageService.findById(message.getId()).getContent());

        // 메시지 삭제 동작도 확인합니다. 사용자와 채널은 파일에 남겨 재실행 때 확인할 수 있습니다.
        messageService.delete(message.getId());
        boolean messageDeleted = messageService.findAll().stream()
                .noneMatch(savedMessage -> savedMessage.getId().equals(message.getId()));
        System.out.println("메시지 삭제: " + (messageDeleted ? "성공" : "실패"));

        // leaveChannel(...)은 참여 관계를 제거합니다. 이 JCF 구현의 변경은 파일에 저장되지 않습니다.
        participationService.leaveChannel(user.getId(), channel.getId());
        boolean left = !participationService
                .findParticipantIds(channel.getId())
                .contains(user.getId());
        System.out.println("채널 탈퇴: " + (left ? "성공" : "실패"));

        System.out.println("\n========== 재실행 확인 정보 ==========");
        System.out.println("사용자 ID: " + user.getId());
        System.out.println("채널 ID: " + channel.getId());
        System.out.println("사용자와 채널 파일은 data/file-demo 아래에 남아 있습니다.");
        System.out.println("프로그램을 다시 실행하면 시작 부분의 저장 개수에 누적 데이터가 표시됩니다.");
        System.out.println("채널 참여 관계는 JCF 구현이므로 재실행 후에는 유지되지 않습니다.");
    }
}
