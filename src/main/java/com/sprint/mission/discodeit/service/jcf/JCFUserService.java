package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JCFUserService implements UserService {

    // Map<K,V>
    private final Map<UUID, User> data;

    public JCFUserService() {
        this.data = new HashMap<>();
}

    // 사용자 생성
    @Override
    public void create(User user) {
        if (data.containsKey(user.getId())) {
            throw new IllegalArgumentException("이미 등록된 ID입니다: " + user.getId());
        }

        if (isEmailDuplicate(user.getEmail(), null)) {
            throw new IllegalArgumentException(
                    "Email is already in use." + user.getEmail()
            );
        }

        data.put(user.getId(), user);
    }

    // 한명 조회
    @Override
    public User findById(UUID id) {
        User user = data.get(id);

        if (user == null) {
            throw new IllegalArgumentException(
                    "Not Found user id: " + id
            );
        }

        return user;
    }

    // 전체 조회
    @Override
    public List<User> findAll() {
        return List.copyOf(data.values()); // 결과를 수정할 수 없는 리스트로 반환
    }

    // update
    @Override
    public void update(UUID id, User user) {
        User existingUser = data.get(id);

        if (existingUser == null) {
            throw new IllegalArgumentException(
                    "Not Found User: " + id
            );
        }

        if (isEmailDuplicate(user.getEmail(), id)) {
            throw new IllegalArgumentException(
                    "Email is already in use." + user.getEmail()
            );
        }

        existingUser.update(
                user.getUsername(),
                user.getEmail(),
                user.getPassword()
        );
    }

    // 사용자 삭제
    @Override
    public void delete(UUID id) {
        User removedUser = data.remove(id);
        if (removedUser == null) {
            throw new IllegalArgumentException(
                    "Not Found User: " + id
            );
        }
    }

    // 이메일 중복 검사 메서드 추가
    private boolean isEmailDuplicate(String email, UUID excludUserId) {
        return data.values().stream()
                .anyMatch(user ->
                        user.getEmail().equalsIgnoreCase(email)
                                && !user.getId().equals(excludUserId)
                );
    }

}
