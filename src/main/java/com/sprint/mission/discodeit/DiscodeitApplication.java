package com.sprint.mission.discodeit;

import com.sprint.mission.discodeit.dto.*;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.AuthService;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

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
		ChannelRepository channelRepository = context.getBean(ChannelRepository.class);
		MessageRepository messageRepository = context.getBean(MessageRepository.class);
		ReadStatusRepository readStatusRepository = context.getBean(ReadStatusRepository.class);
		UserStatusRepository userStatusRepository =
				context.getBean(UserStatusRepository.class);

		AuthService authService = context.getBean(AuthService.class);

		verifyUserService(userService, binaryContentRepository, userStatusRepository);

		//SetUp
		UserDto user = setupUser(userService);

		verifyAuthService(authService, user);

		Channel channel = setupChannel(channelService);
		verifyChannelService(
				channelService,
				userService,
				user,
				channelRepository,
				messageRepository,
				readStatusRepository
		);

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
		return channelService.createPublic(
				new PublicChannelCreateRequest("공지", "공지 채널입니다.")
		);
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

	/** 채널 생성, 조회, 수정, 삭제의 고도화 요구사항을 기능별로 확인합니다. */
	private static void verifyChannelService(
			ChannelService channelService,
			UserService userService,
			UserDto firstUser,
			ChannelRepository channelRepository,
			MessageRepository messageRepository,
			ReadStatusRepository readStatusRepository
	) {
		String runId = UUID.randomUUID().toString().substring(0, 8);
		UserDto secondUser = setupVerificationUser(userService, runId, "second");
		UserDto outsider = setupVerificationUser(userService, runId, "outsider");
		UUID publicChannelId = null;
		UUID privateChannelId = null;
		UUID latestMessageId = null;

		try {
			System.out.println("\n=== ChannelService 검증: 채널 생성 ===");
			Channel publicChannel = channelService.createPublic(
					new PublicChannelCreateRequest("공개-" + runId, "공개 채널 검증")
			);
			publicChannelId = publicChannel.getId();
			UUID verifiedPublicChannelId = publicChannelId;
			Channel privateChannel = channelService.createPrivate(
					new PrivateChannelCreateRequest(
							Arrays.asList(firstUser.id(), secondUser.id())
					)
			);
			privateChannelId = privateChannel.getId();
			UUID verifiedPrivateChannelId = privateChannelId;
			check(publicChannel.getType() == ChannelType.PUBLIC, "PUBLIC 채널 생성");
			check(privateChannel.getType() == ChannelType.PRIVATE
					&& privateChannel.getName() == null
					&& privateChannel.getDescription() == null,
					"PRIVATE 채널은 이름과 설명 없이 생성");
			check(readStatusRepository.findByUserIdAndChannelId(firstUser.id(), privateChannelId).isPresent()
					&& readStatusRepository.findByUserIdAndChannelId(secondUser.id(), privateChannelId).isPresent(),
					"PRIVATE 채널 참여자별 ReadStatus 생성");

			System.out.println("\n=== ChannelService 검증: 채널 조회 DTO ===");
			Message latestMessage = new Message("최근 메시지 확인", firstUser.id(), publicChannelId);
			messageRepository.save(latestMessage);
			latestMessageId = latestMessage.getId();
			var publicDto = channelService.findById(publicChannelId);
			var privateDto = channelService.findById(privateChannelId);
			check(latestMessage.getCreatedAt().equals(publicDto.lastMessageAt()),
					"채널 DTO에 가장 최근 메시지 시각 포함");
			var publicListDto = channelService.findAllByUserId(firstUser.id()).stream()
					.filter(item -> item.id().equals(verifiedPublicChannelId))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("공개 채널이 사용자 목록에 없습니다."));
			check(latestMessage.getCreatedAt().equals(publicListDto.lastMessageAt()),
					"사용자별 채널 목록 DTO에도 최근 메시지 시각 포함");
			check(privateDto.participantUserIds().containsAll(
					Arrays.asList(firstUser.id(), secondUser.id())),
					"PRIVATE 채널 DTO에 참여자 ID 포함");
			var privateListDto = channelService.findAllByUserId(firstUser.id()).stream()
					.filter(item -> item.id().equals(verifiedPrivateChannelId))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("비공개 채널이 참여자 목록에 없습니다."));
			check(privateListDto.participantUserIds().containsAll(
					Arrays.asList(firstUser.id(), secondUser.id())),
					"사용자별 채널 목록 DTO에도 PRIVATE 참여자 ID 포함");

			System.out.println("\n=== ChannelService 검증: 사용자별 채널 목록 ===");
			var firstUserChannels = channelService.findAllByUserId(firstUser.id());
			var outsiderChannels = channelService.findAllByUserId(outsider.id());
			check(firstUserChannels.stream().anyMatch(item -> item.id().equals(verifiedPublicChannelId))
					&& outsiderChannels.stream().anyMatch(item -> item.id().equals(verifiedPublicChannelId)),
					"PUBLIC 채널은 모든 사용자 목록에 포함");
			check(firstUserChannels.stream().anyMatch(item -> item.id().equals(verifiedPrivateChannelId))
					&& outsiderChannels.stream().noneMatch(item -> item.id().equals(verifiedPrivateChannelId)),
					"PRIVATE 채널은 참여자에게만 노출");

			System.out.println("\n=== ChannelService 검증: 수정 및 삭제 ===");
			channelService.update(new ChannelUpdateRequest(
					verifiedPublicChannelId, "수정된 공개-" + runId, "공개 채널 수정 검증"
			));
			check(channelService.findById(verifiedPublicChannelId).name().equals("수정된 공개-" + runId),
					"PUBLIC 채널 수정");
			expectIllegalArgument(
					() -> channelService.update(new ChannelUpdateRequest(
							verifiedPrivateChannelId, "수정 시도", "수정 불가 확인")),
					"PRIVATE 채널 수정 거부"
			);
			channelService.delete(privateChannelId);
			privateChannelId = null;
			check(channelRepository.findById(privateChannel.getId()).isEmpty(), "채널 삭제");
			check(readStatusRepository.findByUserIdAndChannelId(firstUser.id(), privateChannel.getId()).isEmpty()
					&& readStatusRepository.findByUserIdAndChannelId(secondUser.id(), privateChannel.getId()).isEmpty(),
					"채널 삭제 시 관련 ReadStatus도 삭제");

			channelService.delete(publicChannelId);
			publicChannelId = null;
			check(channelRepository.findById(publicChannel.getId()).isEmpty()
					&& messageRepository.findById(latestMessage.getId()).isEmpty(),
					"채널 삭제 시 채널과 관련 메시지 삭제");
		} finally {
			if (privateChannelId != null) {
				cleanupChannel(channelService, privateChannelId);
			}
			if (publicChannelId != null) {
				cleanupChannel(channelService, publicChannelId);
			}
			if (latestMessageId != null) {
				messageRepository.deleteById(latestMessageId);
			}
			cleanupUser(userService, secondUser.id());
			cleanupUser(userService, outsider.id());
		}
	}

	private static UserDto setupVerificationUser(UserService userService, String runId, String suffix) {
		return userService.create(new UserCreateRequest(
				"channel-check-" + suffix + "-" + runId,
				"channel-check-" + suffix + "-" + runId + "@codeit.com",
				"password",
				null
		));
	}

	private static void cleanupChannel(ChannelService channelService, UUID channelId) {
		try {
			channelService.delete(channelId);
		} catch (IllegalArgumentException ignored) {
			// 검증 도중 이미 삭제된 채널은 다시 정리할 필요가 없습니다.
		}
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
			System.out.println("\n=== UserService 검증: 사용자 생성과 상태 ===");
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

			System.out.println("\n=== UserService 검증: 조회 DTO ===");
			UserDto foundUser = userService.findById(profileUser.id());
			check(foundUser.id().equals(profileUser.id()) && foundUser.online(),
					"findById가 온라인 상태를 포함한 DTO를 반환");
			check(userService.findAll().stream()
					.anyMatch(user -> user.id().equals(noProfileUser.id()) && user.online()),
					"findAll이 온라인 상태를 포함한 사용자 DTO 목록을 반환");
			check(Arrays.stream(UserDto.class.getRecordComponents())
					.noneMatch(component -> component.getName().equals("password")),
					"UserDto에서 비밀번호 제외");

			System.out.println("\n=== UserService 검증: 중복 username/email 거부 ===");
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

			System.out.println("\n=== UserService 검증: 프로필 수정 ===");
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

			System.out.println("\n=== UserService 검증: 연관 데이터와 사용자 삭제 ===");
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

	private static void verifyAuthService(AuthService authService, UserDto user) {
		AuthUserDto loggedInUser = authService.login(
				new AuthLoginRequest(user.username(), "woody1234")
		);

		check(
				loggedInUser.id().equals(user.id()),
				"올바른 아이디와 비밀번호로 로그인"
		);

		expectIllegalArgument(
				() -> authService.login(
						new AuthLoginRequest("없는사용자", "woody1234")
				),
				"존재하지 않는 아이디로 로그인 실패"
		);

		expectIllegalArgument(
				() -> authService.login(
						new AuthLoginRequest(user.username(), "wrong-password")
				),
				"잘못된 비밀번호로 로그인 실패"
		);
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
}
