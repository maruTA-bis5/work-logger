package net.bis5.worklogger.service;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import net.bis5.mattermost.client4.ApiResponse;
import net.bis5.mattermost.client4.MattermostClient;
import net.bis5.mattermost.model.Post;
import net.bis5.mattermost.model.User;
import net.bis5.worklogger.entity.MattermostConfig;

@ApplicationScoped
public class MattermostNotifyService {

    public void onNotify(@Observes MattermostNotifyEvent event) {
        Optional<MattermostConfig> configOpt = MattermostConfig.findByUser(event.getUser());
        if (configOpt.isEmpty()) {
            return;
        }
        MattermostConfig config = configOpt.get();
        try (var client = MattermostClient.builder().url(config.mattermostUrl).ignoreUnknownProperties().build()) {
            ApiResponse<User> loginResult = client.login(config.mattermostUsername, config.mattermostPassword);
            /* see mattermost4j#358
            if (loginResult.hasError()) {
                return;
            }
            */
            String message = createMessage(event);
            var post = new Post(config.channelId, message);
            client.createPost(post);
            client.logout();
        }
    }

    private String createMessage(MattermostNotifyEvent event) {
        String message;
        switch (event.getType()) {
            case WORK_START:
                message = "出勤";
                break;
            case WORK_END:
                message = "退勤";
                break;
            case BREAK_START:
                message = "休憩";
                break;
            case BREAK_END:
                message = "休憩戻り";
                break;
            case LUNCH_START:
                message = "昼休み";
                break;
            case LUNCH_END:
                message = "昼休み戻り";
                break;
            case PRIVATE_OUT_START:
                message = "私用外出";
                break;
            case PRIVATE_OUT_END:
                message = "私用外出戻り";
                break;
            default:
                throw new IllegalStateException("type is unknown");
        }
        if (event.getCommentary() != null && !event.getCommentary().isEmpty()) {
            message += " (" + event.getCommentary() + ")";
        }
        return message;
    }
}
