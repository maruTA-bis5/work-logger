package net.bis5.worklogger.controller.top;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;

import net.bis5.worklogger.entity.TaskManhourFact;
import net.bis5.worklogger.entity.WorkUser;

@RequestScoped
public class WorkFinishEventConsumer {

    public static class WorkFinishEvent {
        private final WorkUser user;
        public WorkFinishEvent(WorkUser user) {
            this.user = user;
        }
        public WorkUser getUser() {
            return user;
        }
    }
    
    public void onWorkFinish(@Observes WorkFinishEvent event) {
        Optional<TaskManhourFact> currentWork = TaskManhourFact.findCurrentTask(event.getUser());
        currentWork.ifPresent(this::finishWork);
    }
    private void finishWork(TaskManhourFact work) {
        work.workEndAt = ZonedDateTime.now();
        work.merge();
    }
}
