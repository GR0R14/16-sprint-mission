package com.sprint.mission.discodeit.entity;

//사용자들이 메시지를 주고받는 채널 정보

import java.util.UUID;

public class Channel extends BaseEntity{
    private static final long serialVersionUID = 1L;

    // Channel field
    private String name;
    private String description;

    public Channel(String name, String description) {
        super();
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
        updateUpdatedAt();
    }
}

