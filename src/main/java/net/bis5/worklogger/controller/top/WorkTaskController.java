package net.bis5.worklogger.controller.top;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Model;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.primefaces.PrimeFaces;

import net.bis5.worklogger.controller.top.WorkFinishEventConsumer.WorkFinishEvent;
import net.bis5.worklogger.entity.Task;
import net.bis5.worklogger.entity.TaskManhourFact;
import net.bis5.worklogger.entity.WorkUser;

@Model
public class WorkTaskController {

    private final HttpServletRequest request;

    private WorkUser user;

    private List<Task> tasks;

    private Event<WorkFinishEvent> workFinishEventBus;

    @Inject
    public WorkTaskController(HttpServletRequest request, Event<WorkFinishEvent> workFinishEventBus) {
        this.request = request;
        this.workFinishEventBus = workFinishEventBus;
    }

    @PostConstruct
    void initialize() {
        user = WorkUser.findByUserName(request.getRemoteUser()).get();
        tasks = Task.findByUserName(user.userName);
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public Task getCurrentTask() {
        return TaskManhourFact.findCurrentTask(user).map(f -> f.task).orElseGet(this::createEmptyTask);
    }

    private Task createEmptyTask() {
        var task = new Task();
        task.taskCode = "xxx";
        task.taskName = "未割当";
        return task;
    }

    @Transactional
    public void startTask(Task task) {
        finishWork();

        var nextWork = new TaskManhourFact();
        nextWork.task = task;
        nextWork.targetDate = LocalDate.now();
        nextWork.workStartAt = ZonedDateTime.now();
        nextWork.persist();
    }

    @Transactional
    public void finishWork() {
        workFinishEventBus.fire(new WorkFinishEvent(user));
    }

    public List<TaskManhourFactSummary> getTodaysManhourFactSummary() {
        Map<Long, Optional<TaskManhourFactSummary>> summaries = TaskManhourFact.findByTargetDate(user, LocalDate.now()).stream()
            .map(TaskManhourFactSummary::new)
            .collect(Collectors.groupingBy(t -> t.task.id, Collectors.reducing(TaskManhourFactSummary::add)));
        return summaries.entrySet().stream()
            .filter(e -> e.getValue().isPresent())
            .map(Map.Entry::getValue)
            .map(Optional::get)
            .sorted(Comparator.comparing(s -> s.task.taskCode))
            .collect(Collectors.toList());
    }

    public static class TaskManhourFactSummary {
        
        private final Task task;
        private final int mins;

        private TaskManhourFactSummary(Task task, int mins) {
            this.task = task;
            this.mins = mins;
        }

        private TaskManhourFactSummary(TaskManhourFact manhour) {
            this.task = manhour.task;
            if (manhour.workEndAt == null) {
                var workDuration = Duration.between(manhour.workStartAt, ZonedDateTime.now());
                long secs = workDuration.getSeconds();
                mins = (int) (secs / 60);
            } else {
                mins = manhour.totalMins;
            }
        }

        public TaskManhourFactSummary add(TaskManhourFactSummary manhour) {
            return new TaskManhourFactSummary(this.task, this.mins + manhour.mins);
        }

        public String toHHMM() {
            var hour = mins / 60;
            var min = mins % 60;
            return String.format("%d:%02d", hour, min);
        }

        public Task getTask() {
            return task;
        }
    }

    public void openReport() {
        PrimeFaces.current().dialog().openDynamic("manhourReport");
    }
}