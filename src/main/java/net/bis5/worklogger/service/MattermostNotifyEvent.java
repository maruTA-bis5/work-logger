package net.bis5.worklogger.service;

import net.bis5.worklogger.entity.WorkUser;

public class MattermostNotifyEvent {

    private final WorkUser user;
    private final PostType type;
    private final String commentary;

    public MattermostNotifyEvent(WorkUser user, PostType type, String commentary) {
        this.user = user;
        this.type = type;
        this.commentary = commentary;
    }

    public enum PostType {
        WORK_START, WORK_END, BREAK_START, BREAK_END, LUNCH_START, LUNCH_END, PRIVATE_OUT_START, PRIVATE_OUT_END;
    }

    public WorkUser getUser() {
        return user;
    }
    public PostType getType() {
        return type;
    }
    public String getCommentary() {
        return commentary;
    }
}
