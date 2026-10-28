package com.sprint.mission.discodeit;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.UUID;

@SpringBootApplication
public class DiscodeitApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context =
				SpringApplication.run(DiscodeitApplication.class, args);
		// 서비스 초기화
		UserService userService = context.getBean(UserService.class);
		ChannelService channelService = context.getBean(ChannelService.class);
		MessageService messageService= context.getBean(MessageService.class);

		//SetUp
		User user = setupUser(userService);
		Channel channel = setupChannel(channelService);
		//Test
		messageCreateTest(messageService, channel, user);
	}

	static User setupUser(UserService userService) {
		// 매 실행마다 이메일을 다르게 만들어 File 저장소를 재실행해도 중복되지 않게 합니다.
		String runId = UUID.randomUUID().toString().substring(0, 8);
		return userService.create("woody-" + runId,
				"woody-" + runId + "@codeit.com", "woody1234");
	}

	static Channel setupChannel(ChannelService channelService) {
		return channelService.create(ChannelType.PUBLIC, "공지", "공지 채널입니다.");
	}

	static void messageCreateTest(
			MessageService messageService,
			Channel channel,
			User author
	) {
		// 템플릿의 인자 순서는 content, channelId, authorId입니다.
		Message message = messageService.create(
				"안녕하세요.", channel.getId(), author.getId()
		);
		System.out.println("메시지 생성: " + message.getId());
	}

	private static void runScenario(
			UserService userService,
			ChannelService channelService,
			MessageService messageService
	) {
		System.out.println("실행 전 사용자 수: " + userService.findAll().size());
		System.out.println("실행 전 채널 수: " + channelService.findAll().size());
		System.out.println("실행 전 메시지 수: " + messageService.findAll().size());

		User user = setupUser(userService);
		Channel channel = setupChannel(channelService);
		messageCreateTest(messageService, channel, user);

		System.out.println("실행 후 사용자 수: " + userService.findAll().size());
		System.out.println("실행 후 채널 수: " + channelService.findAll().size());
		System.out.println("실행 후 메시지 수: " + messageService.findAll().size());
	}
}
