package net.bis5.worklogger.service.export;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.bbreak.excella.reports.exporter.ExcelExporter;
import org.bbreak.excella.reports.listener.ReportProcessAdaptor;
import org.bbreak.excella.reports.model.ReportBook;
import org.bbreak.excella.reports.model.ReportSheet;
import org.bbreak.excella.reports.processor.ReportProcessor;
import org.bbreak.excella.reports.tag.SingleParamParser;

import net.bis5.worklogger.entity.Task;
import net.bis5.worklogger.entity.WorkUser;
import net.bis5.worklogger.service.summary.SummaryService;
import net.bis5.worklogger.service.summary.SummaryService.SummaryDay;
import net.bis5.worklogger.service.summary.SummaryService.SummaryTasks;
import net.bis5.worklogger.utils.Format;

@ApplicationScoped
public class ReportExporter {

    private final WorkUser user;

    private final SummaryService summaryService;

    @Inject
    public ReportExporter(HttpServletRequest request, SummaryService summaryService) {
        user = WorkUser.findByUserName(request.getRemoteUser()).get();
        this.summaryService = summaryService;
    }
    public Path createReport(ReportExportParameter parameter, Path templateFilePath) {
        ReportProcessor processor = new ReportProcessor();
        ReportBook reportBook = createReportBook(parameter);
        reportBook.setTemplateFileName(templateFilePath.toString());
        processor.addReportProcessListener(new ReportProcessAdaptor(){
            @Override
            public void preBookParse(Workbook workbook, ReportBook reportBook) {
                Set<String> usedTemplateSheetNames = reportBook.getReportSheets().stream()
                    .map(ReportSheet::getTemplateName)
                    .collect(Collectors.toSet());
                IntStream.range(0, workbook.getNumberOfSheets())
                    .mapToObj(workbook::getSheetAt)
                    .map(Sheet::getSheetName)
                    .filter(Predicate.not(usedTemplateSheetNames::contains))
                    .map(ReportSheet::new)
                    .forEach(reportBook::addReportSheet);
            }

            @Override
            public void postBookParse(Workbook workbook, ReportBook reportBook) {
                workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            }
        });
        try {
            Path reportFile = Files.createTempFile("report", ".tmp");
            reportBook.setOutputFileName(reportFile.toString());

            processor.process(reportBook);
            Files.delete(templateFilePath);

            return reportFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ReportBook createReportBook(ReportExportParameter parameter) {
        Pair<SummaryTasks, List<SummaryDay>> summary = summaryService.loadSummarySumSameCodeWork(user, parameter.getTargetMonth());
        var reportBook = new ReportBook((String)null, null, ExcelExporter.FORMAT_TYPE);
        var reportSheet = new ReportSheet(parameter.getTemplateSheetName());
        reportBook.addReportSheet(reportSheet);

        List<Task> tasks = summary.getLeft().getTasksOrderByTaskCode();
        Map</*taskId*/Long, /*taskSeq*/Integer> taskSeqById = new HashMap<>();
        for (int i = 0; i < tasks.size(); i++) {
            var task = tasks.get(i);
            int seq = i + 1;
            reportSheet.addParam(SingleParamParser.DEFAULT_TAG, parameter.getTaskCodeTag(seq), task.taskCode);
            reportSheet.addParam(SingleParamParser.DEFAULT_TAG, parameter.getTaskNameTag(seq), task.taskName);
            taskSeqById.put(task.id, seq);
        }

        for (SummaryDay day : summary.getRight()) {
            int dayOfMonth = day.getTargetDate().getDayOfMonth();
            Date juDate = Date.from(day.getTargetDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            reportSheet.addParam(SingleParamParser.DEFAULT_TAG, parameter.getDateTag(dayOfMonth), juDate);
            reportSheet.addParam(SingleParamParser.DEFAULT_TAG, parameter.getWorkStartTimeTag(dayOfMonth), Format.toHHMM(day.getWorkStartAt()));
            reportSheet.addParam(SingleParamParser.DEFAULT_TAG, parameter.getWorkEndTimeTag(dayOfMonth), Format.toHHMM(day.getWorkEndAt()));
            reportSheet.addParam(SingleParamParser.DEFAULT_TAG, parameter.getRestTimeTag(dayOfMonth), Format.toHours(day.getBreakMins()));
            for (Task task : tasks) {
                int taskSeq = taskSeqById.get(task.id);
                reportSheet.addParam(SingleParamParser.DEFAULT_TAG, parameter.getTaskWorkTimeTag(taskSeq, dayOfMonth), Format.toHours(day.getManhourMinsByTaskId(task.id)));
            }
        }

        return reportBook;
    }
}
