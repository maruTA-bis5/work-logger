package net.bis5.worklogger.service.summary;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang3.tuple.Pair;

import net.bis5.worklogger.entity.AttendanceFact;
import net.bis5.worklogger.entity.BreakFact;
import net.bis5.worklogger.entity.BreakKind;
import net.bis5.worklogger.entity.Task;
import net.bis5.worklogger.entity.TaskManhourFact;
import net.bis5.worklogger.entity.WorkUser;

@ApplicationScoped
public class SummaryService {

    public Pair<SummaryTasks, List<SummaryDay>> loadSummarySumSameCodeWork(WorkUser user, YearMonth targetMonth) {
        return loadSummary(user, targetMonth, true);
    }

    public Pair<SummaryTasks, List<SummaryDay>> loadSummary(WorkUser user, YearMonth targetMonth) {
        return loadSummary(user, targetMonth, false);
    }

    private Pair<SummaryTasks, List<SummaryDay>> loadSummary(WorkUser user, YearMonth targetMonth, boolean sumSameCodeWork) {
        List<AttendanceFact> attendanceFacts = AttendanceFact.findByYearMonth(user, targetMonth);
        List<BreakFact> breakFacts = BreakFact.findByYearMonth(user, targetMonth);
        List<TaskManhourFact> manhourFacts = TaskManhourFact.findByYearMonth(user, targetMonth);
        Map<LocalDate, AttendanceFact> attendanceByDate = attendanceFacts.stream()
            .collect(Collectors.toMap(a -> a.targetDate, a -> a));
        Map<LocalDate, List<BreakFact>> breaksByDate = breakFacts.stream()
            .collect(Collectors.groupingBy(b -> b.targetDate, Collectors.toList()));
        Map<LocalDate, Map<Long/*taskId*/, List<TaskManhourFact>>> manhoursByTaskByDate = manhourFacts.stream()
            .collect(Collectors.groupingBy(m -> m.targetDate, Collectors.groupingBy(m -> m.task.id, Collectors.toList())));
        List<Task> tasks = manhoursByTaskByDate.values().stream()
            .map(Map::values)
            .flatMap(Collection::stream)
            .flatMap(List::stream)
            .map(t -> t.task)
            .distinct()
            .collect(Collectors.toList());

        List<SummaryDay> days = new ArrayList<>();
        Map</*taskId*/Long, /*taskCode*/String> taskCodeDic = tasks.stream().collect(Collectors.toMap(t -> t.id, t -> t.taskCode));
        for (LocalDate d = targetMonth.atDay(1); YearMonth.from(d).equals(targetMonth); d = d.plusDays(1)) {
            Map</*taskId*/Long, List<TaskManhourFact>> manhours = manhoursByTaskByDate.getOrDefault(d, Collections.emptyMap());
            if (sumSameCodeWork) {
                // 同一タスクコードのtaskIdにはおなじListをセットしておく
                Map</*taskCode*/String, List<TaskManhourFact>> manhoursByTaskCode
                    = manhours.entrySet().stream()
                        .map(e -> Pair.of(taskCodeDic.get(e.getKey()), e.getValue()))
                        .collect(Collectors.groupingBy(Pair::getKey, Collectors.flatMapping(e -> e.getValue().stream(), Collectors.toList())));
                manhours = manhours.entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), manhoursByTaskCode.getOrDefault(taskCodeDic.get(e.getKey()), Collections.emptyList())))
                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
            }
            days.add(new SummaryDay(d, attendanceByDate.get(d), breaksByDate.getOrDefault(d, Collections.emptyList()), manhours));
        }
        if (sumSameCodeWork) {
            Map</*taskCode*/String, Task> tasksToDisplay = new HashMap<>();
            for (var task : tasks) {
                String code  = task.taskCode;
                Task toDisplay = tasksToDisplay.computeIfAbsent(code, c -> { var t = new Task(); t.id = task.id; t.taskCode = c; return t;});
                if (toDisplay.taskName == null) {
                    toDisplay.taskName = task.taskName;
                } else {
                    toDisplay.taskName += "," + task.taskName;
                }
                tasksToDisplay.put(code, toDisplay);
            }
            tasks = new ArrayList<>(tasksToDisplay.values());
        }

        var summaryTasks = new SummaryTasks(tasks);
        days.sort(Comparator.comparing(SummaryDay::getTargetDate));
        return Pair.of(summaryTasks, days);
    }

    public static class SummaryTasks {
        private final Map</*taskId*/Long, Task> taskById;

        SummaryTasks(List<Task> tasks) {
            this.taskById = tasks.stream()
                .collect(Collectors.toMap(t -> t.id, t -> t));
        }

        public List<Task> getTasksOrderByTaskCode() {
            return taskById.values().stream()
                .sorted(Comparator.comparing(t -> t.taskCode))
                .collect(Collectors.toList());
        }
    }

    public static class SummaryDay {
        private final LocalDate targetDate;
        private final AttendanceFact attendance;
        private final List<BreakFact> breaks;
        private final Map</*taskId*/Long, List<TaskManhourFact>> manhours;
        SummaryDay(LocalDate targetDate, AttendanceFact attendance, List<BreakFact> breaks, Map<Long, List<TaskManhourFact>> manhours) {
            this.targetDate = targetDate;
            this.attendance = Objects.requireNonNullElseGet(attendance, AttendanceFact::new);
            this.breaks = breaks;
            this.manhours = manhours;
        }

        public LocalDate getTargetDate() {
            return targetDate;
        }

        public ZonedDateTime getWorkStartAt() {
            return attendance.workStartAt;
        }

        public ZonedDateTime getWorkEndAt() {
            return attendance.workEndAt;
        }

        public int getBreakMins() {
            return getBreakMins(BreakKind.BREAK);
        }

        public int getLunchMins() {
            return getBreakMins(BreakKind.LUNCH);
        }

        public int getPrivateOutMins() {
            return getBreakMins(BreakKind.PRIVATE_OUT);
        }

        private int getBreakMins(BreakKind kind) {
            return breaks.stream()
                .filter(b -> b.breakKind == kind)
                .mapToInt(b -> b.totalMins)
                .sum();
        }

        public int getTotalWorkMins() {
            return attendance.totalMins - getBreakMins() - getLunchMins() - getPrivateOutMins();
        }

        public int getManhourMinsByTaskId(long taskId) {
            return manhours.getOrDefault(taskId, Collections.emptyList())
                .stream()
                .mapToInt(m -> m.totalMins)
                .sum();
        }

        public Collection<TaskManhourFact> getManhours() {
            return manhours.values().stream().flatMap(List::stream).collect(Collectors.toList());
        }
    }
}
