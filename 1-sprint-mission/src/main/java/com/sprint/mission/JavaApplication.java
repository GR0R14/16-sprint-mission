package com.sprint.mission;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.jcf.JCFUserService;

import java.util.UUID;

public class JavaApplication {

    public static void main(String[] args) {

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
        System.out.println("delete check: " + userService.findById(userId));





    }
}
