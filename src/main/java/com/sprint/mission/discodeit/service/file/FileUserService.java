package com.sprint.mission.discodeit.service.file;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.UserService;

import java.util.List;
import java.util.UUID;

/** User의 업무 규칙을 적용하고 실제 저장 작업은 UserRepository 구현체에 위임합니다. */
public class FileUserService implements UserService {

    // 이메일 규칙 등 사용자 비즈니스 로직은 서비스에 두고,
    // 파일 저장과 조회는 주입받은 FileUserRepository에 위임합니다.
    private final UserRepository userRepository;

    public FileUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void create(User user) {
        // 입력과 중복 규칙을 먼저 검사하고, 모두 통과한 경우에만 저장소에 기록합니다.
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }

        if (userRepository.existsById(user.getId())) {
            throw new IllegalArgumentException(
                    "이미 등록된 ID입니다: " + user.getId()
            );
        }

        validateEmail(user.getEmail());

        if (isEmailDuplicate(user.getEmail(), null)) {
            throw new IllegalArgumentException(
                    "Email is already in use. " + user.getEmail()
            );
        }

        userRepository.save(user);
    }

    @Override
    public User findById(UUID id) {
        // Repository의 Optional 결과를 Service API에 맞게 찾지 못함 예외로 바꿉니다.
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Not Found user id: " + id
                ));
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public void update(UUID id, User user) {
        // 기존 객체를 조회하고 검증한 다음 변경 내용을 같은 ID로 다시 저장합니다.
        User existingUser = findById(id);

        validateEmail(user.getEmail());

        if (isEmailDuplicate(user.getEmail(), id)) {
            throw new IllegalArgumentException(
                    "Email is already in use. " + user.getEmail()
            );
        }

        existingUser.update(
                user.getUsername(),
                user.getEmail(),
                user.getPassword()
        );

        userRepository.save(existingUser);
    }

    @Override
    public void delete(UUID id) {
        // 저장소가 삭제에 실패하면 Service API에서는 존재하지 않는 사용자로 처리합니다.
        if (!userRepository.deleteById(id)) {
            throw new IllegalArgumentException("Not Found User: " + id);
        }
    }

    private boolean isEmailDuplicate(String email, UUID excludeUserId) {
        return userRepository.findAll().stream()
                .anyMatch(user ->
                        user.getEmail().equalsIgnoreCase(email)
                                && !user.getId().equals(excludeUserId)
                );
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 비어 있을 수 없습니다.");
        }

        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!email.matches(emailPattern)) {
            throw new IllegalArgumentException(
                    "올바른 이메일 형식이 아닙니다: " + email
            );
        }
    }
}
