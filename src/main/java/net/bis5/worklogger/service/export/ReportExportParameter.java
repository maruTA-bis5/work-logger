package net.bis5.worklogger.service.export;

import java.io.Serializable;
import java.time.YearMonth;

public class ReportExportParameter implements Serializable {

    private YearMonth targetMonth;
    private String dateTag;
    private String workStartTimeTag;
    private String workEndTimeTag;
    private String restTimeTag;
    private String taskCodeTag;
    private String taskNameTag;
    private String taskWorkTimeTag;
    private String templateSheetName;

    private static final String SEQ_PLACEHOLDER = "__N__";
    private static final String TASK_SEQ = "__M__";

    private String replace(String tag, int dayOfMonth) {
        return tag.replace(SEQ_PLACEHOLDER, String.valueOf(dayOfMonth));
    }

    public YearMonth getTargetMonth() { return targetMonth; }
    public void setTargetMonth(YearMonth targetMonth) { this.targetMonth = targetMonth; }
    public String getDateTag() { return dateTag; }
    public String getDateTag(int dayOfMonth) { return replace(dateTag, dayOfMonth); }
    public void setDateTag(String dateTag) { this.dateTag = dateTag; }
    public String getWorkStartTimeTag() { return workStartTimeTag; }
    public String getWorkStartTimeTag(int dayOfMonth) { return replace(workStartTimeTag, dayOfMonth); }
    public void setWorkStartTimeTag(String workStartTimeTag) { this.workStartTimeTag = workStartTimeTag; }
    public String getWorkEndTimeTag() { return workEndTimeTag; }
    public String getWorkEndTimeTag(int dayOfMonth) { return replace(workEndTimeTag, dayOfMonth); }
    public void setWorkEndTimeTag(String workEndTimeTag) { this.workEndTimeTag = workEndTimeTag; }
    public String getRestTimeTag() { return restTimeTag; }
    public String getRestTimeTag(int dayOfMonth) { return replace(restTimeTag, dayOfMonth); }
    public void setRestTimeTag(String restTimeTag) { this.restTimeTag = restTimeTag; }
    public String getTaskCodeTag() { return taskCodeTag; }
    public String getTaskCodeTag(int dayOfMonth) { return replace(taskCodeTag, dayOfMonth); }
    public void setTaskCodeTag(String taskCodeTag) { this.taskCodeTag = taskCodeTag; }
    public String getTaskNameTag() { return taskNameTag; }
    public String getTaskNameTag(int dayOfMonth) { return replace(taskNameTag, dayOfMonth); }
    public void setTaskNameTag(String taskNameTag) { this.taskNameTag = taskNameTag; }
    public String getTaskWorkTimeTag() { return taskWorkTimeTag; }
    public String getTaskWorkTimeTag(int taskSeq, int dayOfMonth) { return replace(taskWorkTimeTag, dayOfMonth).replace(TASK_SEQ, String.valueOf(taskSeq)); }
    public void setTaskWorkTimeTag(String taskWorkTimeTag) { this.taskWorkTimeTag = taskWorkTimeTag; }
    public String getTemplateSheetName() { return templateSheetName; }
    public void setTemplateSheetName(String templateSheetName) { this.templateSheetName = templateSheetName; }

}
