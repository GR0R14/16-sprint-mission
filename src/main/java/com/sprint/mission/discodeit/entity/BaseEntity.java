package com.sprint.mission.discodeit.entity;

import java.io.Serializable;
import java.util.UUID;

public abstract class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final Long createdAt;
    private Long updatedAt;

    protected BaseEntity() {
        this.id = UUID.randomUUID(); // 객체를 생성할 때 새로운 식별자를 발급
        this.createdAt = System.currentTimeMillis(); // 밀리초 단위
        this.updatedAt = this.createdAt; // 처음 생성됐을 때 생성 시간과 수정 시간이 같도록 설정
    }

    public UUID getId() { return id; }
    public Long getCreatedAt() { return createdAt; }
    public Long getUpdatedAt() { return updatedAt; }

    protected void updateUpdatedAt() {
        this.updatedAt = System.currentTimeMillis();
    }

}
