package net.bis5.worklogger.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.inject.Model;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;

import net.bis5.worklogger.entity.AttendanceFact;
import net.bis5.worklogger.entity.BreakFact;
import net.bis5.worklogger.entity.BreakKind;
import net.bis5.worklogger.entity.Task;
import net.bis5.worklogger.entity.TaskManhourFact;
import net.bis5.worklogger.entity.WorkUser;

@Model
public class MonthlyReportView {

    private final WorkUser user;

    @Inject
    public MonthlyReportView(HttpServletRequest request) {
        user = WorkUser.findByUserName(request.getRemoteUser()).get();
    }

    private YearMonth targetMonth = YearMonth.now();

    public void setTargetMonth(YearMonth targetMonth) {
        this.targetMonth = targetMonth;
    }

    public YearMonth getTargetMonth() {
        return targetMonth;
    }

    private boolean sumSameCodeWork;

    public void setSumSameCodeWork(boolean sumSameCodeWork) {
        this.sumSameCodeWork = sumSameCodeWork;
    }

    public boolean isSumSameCodeWork() {
        return sumSameCodeWork;
    }

    public void refresh() {
        List<AttendanceFact> attendanceFacts = AttendanceFact.findByYearMonth(user, targetMonth);
        List<BreakFact> breakFacts = BreakFact.findByYearMonth(user, targetMonth);
        List<TaskManhourFact> manhourFacts = TaskManhourFact.findByYearMonth(user, targetMonth);
        Map<LocalDate, AttendanceFact> attendanceByDate = attendanceFacts.stream()
            .collect(Collectors.toMap(a -> a.targetDate, a -> a));
        Map<LocalDate, List<BreakFact>> breaksByDate = breakFacts.stream()
            .collect(Collectors.groupingBy(b -> b.targetDate, Collectors.toList()));
        Map<LocalDate, Map<Long/*taskId*/, List<TaskManhourFact>>> manhoursByTaskByDate = manhourFacts.stream()
            .collect(Collectors.groupingBy(m -> m.targetDate, Collectors.groupingBy(m -> m.task.id, Collectors.toList())));
        tasks = manhoursByTaskByDate.values().stream()
            .map(Map::values)
            .flatMap(Collection::stream)
            .flatMap(List::stream)
            .map(t -> t.task)
            .distinct()
            .collect(Collectors.toList());

        rows = new ArrayList<>();
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
            rows.add(new ReportRow(d, attendanceByDate.get(d), breaksByDate.getOrDefault(d, Collections.emptyList()), manhours));
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
            tasks.sort(Comparator.comparing(t -> t.taskCode));

            manhourMinsByTask = manhoursByTaskByDate.values().stream()
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .map(e -> Pair.of(taskCodeDic.get(e.getKey()), e.getValue())) // Stream<Pair<taskCode, List<TaskManhourFact>>>
                .map(e -> Pair.of(tasksToDisplay.get(e.getKey()), e.getValue()))
                .collect(Collectors.groupingBy(e -> e.getKey().id, 
                    Collectors.flatMapping(e -> e.getValue().stream(), Collectors.summingInt(m -> m.totalMins))));
        } else {
            manhourMinsByTask = manhoursByTaskByDate.values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(m -> m.task.id, Collectors.summingInt(m -> m.totalMins)));
        }
    }

    private List<ReportRow> rows;
    private List<Task> tasks;
    private Map<Long/*taskId*/, Integer/*mins*/> manhourMinsByTask;

    public List<ReportRow> getRows() {
        return rows;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public int getManhourMins(Long taskId) {
        return manhourMinsByTask.getOrDefault(taskId, 0);
    }

    public static class ReportRow {
        private final LocalDate targetDate;
        private final AttendanceFact attendance;
        private final List<BreakFact> breaks;
        private final Map<Long/*taskId*/, List<TaskManhourFact>> manhours;

        private ReportRow(LocalDate targetDate, AttendanceFact attendance, List<BreakFact> breaks, Map<Long, List<TaskManhourFact>> manhours) {
            this.targetDate = targetDate;
            this.attendance = attendance;
            this.breaks = breaks;
            this.manhours = manhours;
        }

        public LocalDate getTargetDate() {
            return targetDate;
        }

        public AttendanceFact getAttendance() {
            return attendance;
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
            return breaks.stream().filter(b -> b.breakKind == kind).mapToInt(b -> b.totalMins).sum();
        }

        public int getManhourMinsByTaskId(Long taskId) {
            return manhours.getOrDefault(taskId, Collections.emptyList()).stream()
                .mapToInt(m -> m.totalMins)
                .sum();
        }
    }

    public String toHHMM(int mins) {
        var hour = mins / 60;
        var min = mins % 60;
        return String.format("%d:%02d", hour, min);
    }
}
