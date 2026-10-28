package com.sprint.mission.discodeit.entity;

import lombok.Getter;

import java.util.UUID;

@Getter
public class Message extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String content;
    private final UUID userId;
    private final UUID channelId;

    public Message(String content, UUID userId, UUID channelId) {
        super();
        this.content = content;
        this.userId = userId;
        this.channelId = channelId;
    }

    public void update(String content) {
        this.content = content;
        updateUpdatedAt();
    }
}
