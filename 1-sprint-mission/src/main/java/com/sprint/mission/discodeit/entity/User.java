package com.sprint.mission.discodeit.entity;

import java.util.UUID;

//서비스를 사용하는 사용자 정보
public class User {

    // 필드를 private으로 선언해 외부에서 직접 수정하지 못하게 하고,
    // Getter로 값을 읽을 수 있게 setting.
    // 불변 객체로 선언해서 외부 접근 및 값 변경 방지
    private final UUID id;
    private final Long createAt;
    private Long updateAt;

    private String username;
    private String email;
    private String password;

    // 생성자를 통해서만 초기화
    // setter 메서드 제거
    public User(String username, String email, String password) {
        this.id = UUID.randomUUID(); // 객체를 생성할 때 새로운 식별자를 발급
        this.createAt = System.currentTimeMillis(); // 밀리초 단위
        this.updateAt = this.createAt; // 처음 생성됐을 때 생성 시간과 수정 시간이 같도록 설정

        this.username = username;
        this.email = email;
        this.password = password;
    }

    public UUID getId() {
        return id;
    }

    public Long getCreateAt() {
        return createAt;
    }

    public Long getUpdateAt() {
        return updateAt;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void update(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.updateAt = System.currentTimeMillis();
    }

}
