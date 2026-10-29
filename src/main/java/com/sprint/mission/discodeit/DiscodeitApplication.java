package com.sprint.mission.discodeit;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.sprint.mission.discodeit.dto.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.UserCreateRequest;
import com.sprint.mission.discodeit.dto.UserDto;
import com.sprint.mission.discodeit.dto.UserUpdateRequest;

import java.util.Arrays;
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
		BinaryContentRepository binaryContentRepository =
				context.getBean(BinaryContentRepository.class);
		UserStatusRepository userStatusRepository =
				context.getBean(UserStatusRepository.class);

		verifyUserService(userService, binaryContentRepository, userStatusRepository);

		//SetUp
		UserDto user = setupUser(userService);
		Channel channel = setupChannel(channelService);
		//Test
		messageCreateTest(messageService, channel, user);
	}

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

	private static void verifyUserService(
			UserService userService,
			BinaryContentRepository binaryContentRepository,
			UserStatusRepository userStatusRepository
	) {
		String runId = UUID.randomUUID().toString().substring(0, 8);
		String username = "dto-check-" + runId;
		String email = username + "@codeit.com";
		UUID profileUserId = null;
		UUID noProfileUserId = null;

		try {
			BinaryContentCreateRequest initialImage = new BinaryContentCreateRequest(
					"profile.png",
					"image/png",
					new byte[]{1, 2, 3}
			);
			UserDto profileUser = userService.create(new UserCreateRequest(
					username, email, "password-1", initialImage
			));
			profileUserId = profileUser.id();

			UserDto noProfileUser = userService.create(new UserCreateRequest(
					username + "-second",
					runId + "-second@codeit.com",
					"password-2",
					null
			));
			noProfileUserId = noProfileUser.id();

			check(profileUser.profileId() != null, "생성 시 프로필 이미지를 선택적으로 등록");
			check(binaryContentRepository.findById(profileUser.profileId()).isPresent(),
					"프로필 이미지가 저장소에 저장됨");
			check(noProfileUser.profileId() == null, "프로필 이미지 없이 사용자 생성");
			check(profileUser.online(), "사용자 생성 시 온라인 상태 포함");
			check(userStatusRepository.findByUserId(profileUser.id()).isPresent(),
					"사용자 생성 시 UserStatus 생성");

			UserDto foundUser = userService.findById(profileUser.id());
			check(foundUser.id().equals(profileUser.id()) && foundUser.online(),
					"findById가 온라인 상태를 포함한 DTO를 반환");
			check(userService.findAll().stream()
					.anyMatch(user -> user.id().equals(noProfileUser.id()) && user.online()),
					"findAll이 온라인 상태를 포함한 사용자 DTO 목록을 반환");
			check(Arrays.stream(UserDto.class.getRecordComponents())
					.noneMatch(component -> component.getName().equals("password")),
					"UserDto에서 비밀번호 제외");

			expectIllegalArgument(
					() -> userService.create(new UserCreateRequest(
							username, runId + "-different@codeit.com", "password", null)),
					"중복 username 생성 거부"
			);
			expectIllegalArgument(
					() -> userService.create(new UserCreateRequest(
							username + "-different", email, "password", null)),
					"중복 email 생성 거부"
			);
			expectIllegalArgument(
					() -> userService.update(new UserUpdateRequest(
							noProfileUser.id(), username, runId + "-update@codeit.com",
							"password", null)),
					"중복 username 수정 거부"
			);
			expectIllegalArgument(
					() -> userService.update(new UserUpdateRequest(
							noProfileUser.id(), username + "-update", email,
							"password", null)),
					"중복 email 수정 거부"
			);

			UUID originalProfileId = profileUser.profileId();
			UserDto updatedWithoutImage = userService.update(new UserUpdateRequest(
					profileUser.id(), username + "-updated", email, "password-updated", null
			));
			check(originalProfileId.equals(updatedWithoutImage.profileId()),
					"수정 요청에 이미지가 없으면 기존 프로필 유지");

			BinaryContentCreateRequest replacementImage = new BinaryContentCreateRequest(
					"replacement.png", "image/png", new byte[]{4, 5, 6}
			);
			UserDto updatedWithImage = userService.update(new UserUpdateRequest(
					profileUser.id(), username + "-updated", email,
					"password-updated", replacementImage
			));
			check(updatedWithImage.username().equals(username + "-updated"),
					"사용자 정보 수정");
			check(!originalProfileId.equals(updatedWithImage.profileId()),
					"수정 시 새 프로필 이미지로 교체");
			check(binaryContentRepository.findById(updatedWithImage.profileId()).isPresent(),
					"교체한 프로필 이미지가 저장됨");

			UUID replacedProfileId = updatedWithImage.profileId();
			userService.delete(profileUser.id());
			profileUserId = null;
			check(binaryContentRepository.findById(replacedProfileId).isEmpty(),
					"사용자 삭제 시 프로필 이미지도 삭제");
			check(userStatusRepository.findByUserId(updatedWithImage.id()).isEmpty(),
					"사용자 삭제 시 UserStatus도 삭제");
			expectIllegalArgument(
					() -> userService.findById(updatedWithImage.id()),
					"삭제한 사용자 조회 실패"
			);

			userService.delete(noProfileUser.id());
			noProfileUserId = null;
			check(userStatusRepository.findByUserId(noProfileUser.id()).isEmpty(),
					"프로필이 없는 사용자 삭제 시 UserStatus도 삭제");
		} finally {
			cleanupUser(userService, profileUserId);
			cleanupUser(userService, noProfileUserId);
		}
	}

	private static void check(boolean condition, String description) {
		if (!condition) {
			throw new IllegalStateException("검증 실패: " + description);
		}
		System.out.println("검증 통과: " + description);
	}

	private static void expectIllegalArgument(Runnable action, String description) {
		try {
			action.run();
		} catch (IllegalArgumentException expected) {
			System.out.println("검증 통과: " + description);
			return;
		}
		throw new IllegalStateException("검증 실패: " + description + "에서 예외가 발생하지 않았습니다.");
	}

	private static void cleanupUser(UserService userService, UUID userId) {
		if (userId == null) {
			return;
		}
		try {
			userService.delete(userId);
		} catch (IllegalArgumentException ignored) {
			// 검증 과정에서 이미 삭제된 사용자는 정리할 필요가 없습니다.
		}
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
