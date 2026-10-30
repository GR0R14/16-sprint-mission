package com.sprint.mission.discodeit;

import com.sprint.mission.discodeit.dto.*;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.AuthService;
import com.sprint.mission.discodeit.service.BinaryContentService;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.ReadStatusService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

@SpringBootApplication
public class DiscodeitApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context =
				SpringApplication.run(DiscodeitApplication.class, args);
		// 서비스 초기화
		UserService userService = context.getBean(UserService.class);
		ChannelService channelService = context.getBean(ChannelService.class);
		MessageService messageService= context.getBean(MessageService.class);
		ReadStatusService readStatusService = context.getBean(ReadStatusService.class);
		UserStatusService userStatusService = context.getBean(UserStatusService.class);
		BinaryContentService binaryContentService = context.getBean(BinaryContentService.class);
		BinaryContentRepository binaryContentRepository =
				context.getBean(BinaryContentRepository.class);
		ChannelRepository channelRepository = context.getBean(ChannelRepository.class);
		MessageRepository messageRepository = context.getBean(MessageRepository.class);
		ReadStatusRepository readStatusRepository = context.getBean(ReadStatusRepository.class);
		UserStatusRepository userStatusRepository =
				context.getBean(UserStatusRepository.class);
		UserRepository userRepository = context.getBean(UserRepository.class);

		AuthService authService = context.getBean(AuthService.class);

		verifyBinaryContentService(binaryContentService, binaryContentRepository);
		verifyUserService(userService, binaryContentRepository, userStatusRepository);
		verifyUserStatusService(userStatusService, userRepository, userStatusRepository);

		//SetUp
		UserDto user = setupUser(userService);
		verifyAuthService(authService, user);

		Channel channel = setupChannel(channelService);
		verifyReadStatusService(readStatusService, user, channel, readStatusRepository);
		verifyChannelService(
				channelService,
				userService,
				user,
				channelRepository,
				messageRepository,
				readStatusRepository
		);

		//Test
		verifyMessageService(messageService, user, channel, channelService, binaryContentRepository);
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

	/** 바이너리 콘텐츠 생성, ID 조회, 여러 ID 조회, 삭제를 확인합니다. */
	private static void verifyBinaryContentService(
			BinaryContentService binaryContentService,
			BinaryContentRepository binaryContentRepository
	) {
		UUID createdContentId = null;
		try {
			System.out.println("\n=== BinaryContentService 검증: 생성과 ID 조회 ===");
			byte[] expectedData = new byte[]{10, 20, 30};
			var created = binaryContentService.create(new BinaryContentCreateRequest(
					"binary-check.bin", "application/octet-stream", expectedData
			));
			createdContentId = created.getId();
			UUID verifiedContentId = createdContentId;
			check(created.getFileName().equals("binary-check.bin")
					&& created.getContentType().equals("application/octet-stream")
					&& Arrays.equals(created.getData(), expectedData),
					"DTO 정보로 바이너리 콘텐츠 생성");
			check(binaryContentService.findById(verifiedContentId).getId()
					.equals(verifiedContentId), "ID로 바이너리 콘텐츠 조회");

			System.out.println("\n=== BinaryContentService 검증: ID 목록 조회 ===");
			UUID missingId = UUID.randomUUID();
			List<UUID> requestedIds = Arrays.asList(verifiedContentId, missingId);
			List<UUID> foundIds = binaryContentService.findAllByIdIn(requestedIds).stream()
					.map(content -> content.getId())
					.toList();
			check(foundIds.size() == 1 && foundIds.contains(verifiedContentId),
					"ID 목록 중 저장된 콘텐츠만 조회");

			System.out.println("\n=== BinaryContentService 검증: 삭제 ===");
			binaryContentService.delete(verifiedContentId);
			createdContentId = null;
			check(binaryContentRepository.findById(verifiedContentId).isEmpty(),
					"ID로 바이너리 콘텐츠 삭제");
		} finally {
			if (createdContentId != null) {
				binaryContentRepository.deleteById(createdContentId);
			}
		}
	}

	/** 사용자 상태의 생성, 조회, 갱신, 삭제 규칙을 확인합니다. */
	private static void verifyUserStatusService(
			UserStatusService userStatusService,
			UserRepository userRepository,
			UserStatusRepository userStatusRepository
	) {
		String runId = UUID.randomUUID().toString().substring(0, 8);
		User testUser = new User(
				"status-check-" + runId,
				"status-check-" + runId + "@codeit.com",
				"password"
		);
		userRepository.save(testUser);
		UUID createdStatusId = null;

		try {
			System.out.println("\n=== UserStatusService 검증: 생성과 중복 방지 ===");
			Instant initialActiveAt = Instant.now().minusSeconds(60);
			UserStatusCreateRequest createRequest = new UserStatusCreateRequest(
					testUser.getId(), initialActiveAt
			);
			UserStatus created = userStatusService.create(createRequest);
			createdStatusId = created.getId();
			UUID verifiedStatusId = createdStatusId;
			check(created.getUserId().equals(testUser.getId())
					&& created.getLastActiveAt().equals(initialActiveAt),
					"사용자 ID와 접속 시각으로 UserStatus 생성");
			expectIllegalArgument(
					() -> userStatusService.create(createRequest),
					"같은 사용자의 중복 UserStatus 생성 거부"
			);
			UUID missingUserId = UUID.randomUUID();
			expectIllegalArgument(
					() -> userStatusService.create(new UserStatusCreateRequest(
							missingUserId, Instant.now())),
					"존재하지 않는 사용자 상태 생성 거부"
			);

			System.out.println("\n=== UserStatusService 검증: ID 및 전체 조회 ===");
			check(userStatusService.findById(verifiedStatusId).getId().equals(verifiedStatusId),
					"ID로 UserStatus 조회");
			check(userStatusService.findAll().stream()
					.anyMatch(item -> item.getId().equals(verifiedStatusId)),
					"모든 UserStatus 조회 결과에 생성한 상태 포함");

			System.out.println("\n=== UserStatusService 검증: ID 및 사용자 ID로 갱신 ===");
			Instant updatedByIdAt = Instant.now().minusSeconds(20);
			userStatusService.update(new UserStatusUpdateRequest(
					verifiedStatusId, updatedByIdAt
			));
			check(userStatusService.findById(verifiedStatusId).getLastActiveAt()
					.equals(updatedByIdAt), "ID와 DTO로 마지막 접속 시각 수정");

			Instant updatedByUserIdAt = Instant.now();
			userStatusService.updateByUserId(testUser.getId(), updatedByUserIdAt);
			check(userStatusService.findById(verifiedStatusId).getLastActiveAt()
					.equals(updatedByUserIdAt), "userId로 해당 사용자의 상태 수정");

			System.out.println("\n=== UserStatusService 검증: 삭제 ===");
			userStatusService.delete(verifiedStatusId);
			createdStatusId = null;
			check(userStatusRepository.findById(verifiedStatusId).isEmpty(),
					"ID로 UserStatus 삭제");
		} finally {
			if (createdStatusId != null) {
				userStatusRepository.deleteById(createdStatusId);
			}
			userRepository.deleteById(testUser.getId());
		}
	}

	/** 읽음 상태의 생성, 중복 방지, 조회, 수정, 삭제를 확인합니다. */
	private static void verifyReadStatusService(
			ReadStatusService readStatusService,
			UserDto user,
			Channel channel,
			ReadStatusRepository readStatusRepository
	) {
		UUID createdReadStatusId = null;
		try {
			System.out.println("\n=== ReadStatusService 검증: 생성과 관계 확인 ===");
			Instant initialReadAt = Instant.now().minusSeconds(30);
			ReadStatusCreateRequest createRequest = new ReadStatusCreateRequest(
					user.id(), channel.getId(), initialReadAt
			);
			ReadStatus created = readStatusService.create(createRequest);
			createdReadStatusId = created.getId();
			UUID verifiedReadStatusId = createdReadStatusId;
			check(created.getUserId().equals(user.id())
					&& created.getChannelId().equals(channel.getId())
					&& created.getLastReadAt().equals(initialReadAt),
					"사용자·채널·읽은 시각으로 ReadStatus 생성");
			expectIllegalArgument(
					() -> readStatusService.create(createRequest),
					"같은 사용자와 채널의 중복 ReadStatus 생성 거부"
			);

			UUID missingUserId = UUID.randomUUID();
			expectIllegalArgument(
					() -> readStatusService.create(new ReadStatusCreateRequest(
							missingUserId, channel.getId(), Instant.now())),
					"존재하지 않는 사용자와 연결된 생성 거부"
			);
			UUID missingChannelId = UUID.randomUUID();
			expectIllegalArgument(
					() -> readStatusService.create(new ReadStatusCreateRequest(
							user.id(), missingChannelId, Instant.now())),
					"존재하지 않는 채널과 연결된 생성 거부"
			);

			System.out.println("\n=== ReadStatusService 검증: ID 및 사용자별 조회 ===");
			check(readStatusService.findById(verifiedReadStatusId).getId()
					.equals(verifiedReadStatusId), "ID로 ReadStatus 조회");
			check(readStatusService.findAllByUserId(user.id()).stream()
					.anyMatch(item -> item.getId().equals(verifiedReadStatusId)),
					"사용자 ID로 ReadStatus 목록 조회");

			System.out.println("\n=== ReadStatusService 검증: 수정과 삭제 ===");
			Instant updatedReadAt = Instant.now();
			readStatusService.update(new ReadStatusUpdateRequest(
					verifiedReadStatusId, updatedReadAt
			));
			check(readStatusService.findById(verifiedReadStatusId).getLastReadAt()
					.equals(updatedReadAt), "마지막 읽은 시각 수정");

			readStatusService.delete(verifiedReadStatusId);
			createdReadStatusId = null;
			check(readStatusRepository.findById(verifiedReadStatusId).isEmpty(),
					"ID로 ReadStatus 삭제");
		} finally {
			if (createdReadStatusId != null) {
				readStatusRepository.deleteById(createdReadStatusId);
			}
		}
	}

	static void messageCreateTest(
			MessageService messageService,
			Channel channel,
			UserDto author
	) {
		Message message = messageService.create(
				new MessageCreateRequest("안녕하세요.", channel.getId(), author.id(), null)
		);
		System.out.println("메시지 생성: " + message.getId());
	}

	/** 첨부파일 생성, 채널별 조회, 수정, 삭제의 동작을 단계별로 확인합니다. */
	private static void verifyMessageService(
			MessageService messageService,
			UserDto author,
			Channel channel,
			ChannelService channelService,
			BinaryContentRepository binaryContentRepository
	) {
		String runId = UUID.randomUUID().toString().substring(0, 8);
		Channel otherChannel = channelService.createPublic(
				new PublicChannelCreateRequest("메시지검증-" + runId, "채널별 조회 검증")
		);
		UUID attachedMessageId = null;
		UUID plainMessageId = null;

		try {
			System.out.println("\n=== MessageService 검증: 첨부파일 선택 등록 ===");
			Message attachedMessage = messageService.create(new MessageCreateRequest(
					"첨부파일 두 개가 있는 메시지",
					channel.getId(),
					author.id(),
					Arrays.asList(
							new BinaryContentCreateRequest("first.txt", "text/plain", new byte[]{1, 2}),
							new BinaryContentCreateRequest("second.png", "image/png", new byte[]{3, 4})
					)
			));
			attachedMessageId = attachedMessage.getId();
			UUID verifiedAttachedMessageId = attachedMessageId;
			check(attachedMessage.getAttachmentIds().size() == 2,
					"메시지에 여러 첨부파일 ID 연결");
			check(attachedMessage.getAttachmentIds().stream()
					.allMatch(id -> binaryContentRepository.findById(id).isPresent()),
					"첨부파일 데이터 저장");

			Message plainMessage = messageService.create(new MessageCreateRequest(
					"첨부파일 없는 메시지",
					otherChannel.getId(),
					author.id(),
					null
			));
			plainMessageId = plainMessage.getId();
			UUID verifiedPlainMessageId = plainMessageId;
			check(plainMessage.getAttachmentIds().isEmpty(), "첨부파일 없이 메시지 생성");

			System.out.println("\n=== MessageService 검증: 채널별 조회 ===");
			List<Message> selectedChannelMessages =
					messageService.findAllByChannelId(channel.getId());
			check(selectedChannelMessages.stream()
					.anyMatch(message -> message.getId().equals(verifiedAttachedMessageId)),
					"지정 채널의 메시지 조회");
			check(selectedChannelMessages.stream()
					.noneMatch(message -> message.getId().equals(verifiedPlainMessageId)),
					"다른 채널의 메시지는 조회 결과에서 제외");

			System.out.println("\n=== MessageService 검증: 메시지 수정 ===");
			messageService.update(new MessageUpdateRequest(attachedMessageId, "수정된 메시지 내용"));
			check(messageService.findById(attachedMessageId).getContent().equals("수정된 메시지 내용"),
					"요청 DTO의 내용으로 메시지 수정");

			System.out.println("\n=== MessageService 검증: 메시지와 첨부파일 삭제 ===");
			List<UUID> attachmentIds = attachedMessage.getAttachmentIds();
			messageService.delete(attachedMessageId);
			attachedMessageId = null;
			check(attachmentIds.stream()
					.allMatch(id -> binaryContentRepository.findById(id).isEmpty()),
					"메시지 삭제 시 연결된 첨부파일도 삭제");
			check(messageService.findAllByChannelId(channel.getId()).stream()
					.noneMatch(message -> message.getId().equals(attachedMessage.getId())),
					"삭제된 메시지는 채널 조회에서 제외");
			messageService.delete(plainMessageId);
			plainMessageId = null;
		} finally {
			if (attachedMessageId != null) {
				cleanupMessage(messageService, attachedMessageId);
			}
			if (plainMessageId != null) {
				cleanupMessage(messageService, plainMessageId);
			}
			channelService.delete(otherChannel.getId());
		}
	}

	private static void cleanupMessage(MessageService messageService, UUID messageId) {
		try {
			messageService.delete(messageId);
		} catch (IllegalArgumentException ignored) {
			// 검증 도중 이미 삭제된 메시지는 다시 정리할 필요가 없습니다.
		}
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
