package net.bis5.worklogger.controller;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import net.bis5.worklogger.entity.Task;
import net.bis5.worklogger.entity.WorkUser;

@Named
@ViewScoped
public class TasksView implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final HttpServletRequest request;
    private WorkUser user;
    @Inject
    public TasksView(HttpServletRequest request) {
        this.request = request;
    }

    @PostConstruct
    void initialize() {
        user = WorkUser.findByUserName(request.getRemoteUser()).get();
        reload();
    }

    private void reload() {
        tasks = Task.findByUserName(user.userName) //
            .stream()
            .map(TaskRow::new)
            .collect(Collectors.toList());
    }

    private List<TaskRow> tasks;

    public List<TaskRow> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskRow> tasks) {
        this.tasks = tasks;
    }

    @Transactional
    public void onSave() {
        tasks.forEach(TaskRow::persist);
        reload();
    }

    public void addTask() {
        var task = new Task();
        task.user = user;
        tasks.add(new TaskRow(task));
    }

    public static class TaskRow implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Task task;

        TaskRow(Task task) {
            this.task = task;
        }
        public Long getId() {
            return task.id;
        }
        public String getTaskCode() {
            return task.taskCode;
        }
        public void setTaskCode(String taskCode) {
            task.taskCode = taskCode;
        }
        public String getTaskName() {
            return task.taskName;
        }
        public void setTaskName(String taskName) {
            task.taskName = taskName;
        }
        public String getDescription() {
            return task.description;
        }
        public void setDescription(String description) {
            task.description = description;
        }
        public LocalDate getAvailableFrom() {
            return task.availableFrom;
        }
        public void setAvailableFrom(LocalDate availableFrom) {
            task.availableFrom = availableFrom;
        }
        public LocalDate getAvailableUntil() {
            return task.availableUntil;
        }
        public void setAvailableUntil(LocalDate availableUntil) {
            task.availableUntil = availableUntil;
        }
        public int getRowKey() {
            return Objects.hashCode(task);
        }
        public Task unwrap() {
            return task;
        }
        private void persist() {
            if (getId() != null) {
                task.merge();
            } else {
                task.persist();
            }
        }
    }
}
