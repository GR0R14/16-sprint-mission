package com.sprint.mission;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import java.util.UUID;

/**
 * JavaApplication_DI에서 사용하는 서비스 검증 시나리오를 모아 둡니다.
 */
public final class ServiceValidationDemo {

    private ServiceValidationDemo() {
    }

    public static void printMessageCreationRejection(
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

        System.out.println(testName
                + " 검증 결과: 예외 발생=" + rejected
                + ", 메시지 저장 여부=" + wasSaved);
    }

    public static void printInvalidEmailCreateRejected(
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

    public static void verifyDuplicateEmailRejected(
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
                .anyMatch(savedUser -> savedUser.getId().equals(user.getId()));

        System.out.println("중복 이메일 생성 거부: "
                + (rejected && !wasSaved));
    }

    public static void verifyDuplicateEmailUpdateRejected(
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
}
