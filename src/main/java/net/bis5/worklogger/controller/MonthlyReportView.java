package net.bis5.worklogger.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.inject.Model;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;

import net.bis5.worklogger.entity.Task;
import net.bis5.worklogger.entity.WorkUser;
import net.bis5.worklogger.service.summary.SummaryService;
import net.bis5.worklogger.service.summary.SummaryService.SummaryDay;
import net.bis5.worklogger.service.summary.SummaryService.SummaryTasks;
import net.bis5.worklogger.utils.Format;

@Model
public class MonthlyReportView {

    private final WorkUser user;

    private final SummaryService summaryService;

    @Inject
    public MonthlyReportView(HttpServletRequest request, SummaryService summaryService) {
        user = WorkUser.findByUserName(request.getRemoteUser()).get();
        this.summaryService = summaryService;
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
        Pair<SummaryTasks, List<SummaryDay>> summary;
        if (isSumSameCodeWork()) {
            summary = summaryService.loadSummarySumSameCodeWork(user, targetMonth);
        } else {
            summary = summaryService.loadSummary(user, targetMonth);
        }
        manhourMinsByTask = summary.getRight().stream()
            .flatMap(d -> d.getManhours().stream())
            .collect(Collectors.groupingBy(m -> m.task.id, Collectors.summingInt(m -> m.totalMins)));
        tasks = summary.getLeft();
        rows = summary.getRight();
    }

    private List<SummaryDay> rows;
    private SummaryTasks tasks;
    private Map<Long/*taskId*/, Integer/*mins*/> manhourMinsByTask;

    public List<SummaryDay> getRows() {
        return rows;
    }

    public List<Task> getTasks() {
        return tasks == null ? List.of() : tasks.getTasksOrderByTaskCode();
    }

    public int getManhourMins(Long taskId) {
        return manhourMinsByTask.getOrDefault(taskId, 0);
    }

    public String toHHMM(int mins) {
        return Format.toHHMM(mins);
    }
}
