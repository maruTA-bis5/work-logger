package net.bis5.worklogger.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import net.bis5.worklogger.service.export.ReportExportParameter;
import net.bis5.worklogger.service.export.ReportExporter;

@SessionScoped
@Named
public class ReportExportView implements Serializable {

    private final ReportExportParameter parameter = new ReportExportParameter();

    private transient UploadedFile templateFile;

    private final ReportExporter exporter;

    @Inject
    public ReportExportView(ReportExporter exporter) {
        this.exporter = exporter;
    }

    @PostConstruct
    void initialize() {
        parameter.setWorkStartTimeTag("workStartTime__N__");
        parameter.setWorkEndTimeTag("workEndTime__N__");
        parameter.setDateTag("date__N__");
        parameter.setTaskCodeTag("taskCode__N__");
        parameter.setTaskNameTag("taskName__N__");
        parameter.setTaskWorkTimeTag("task__M__WorkTime__N__");
    }

    public void processExport() throws IOException {
        if (templateFile == null) { return; }
        Path templateFilePath = extractTemplateToTempFile();
        try {
            Path reportFilePath = exporter.createReport(parameter, templateFilePath);
            reportFilePathStr = reportFilePath.toString() + extractSuffix(templateFile.getFileName());
            downloadFileName = "WorkReport_" + parameter.getTargetMonth() + extractSuffix(templateFile.getFileName());
            PrimeFaces.current().executeScript("PF('downloadTrigger').jq.click();");
        } finally {
            Files.deleteIfExists(templateFilePath);
        }
    }

    private String downloadFileName;
    private String reportFilePathStr;

    private InputStream safeInputStream(Path path) {
        try {
            return Files.newInputStream(path, StandardOpenOption.DELETE_ON_CLOSE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public StreamedContent getReportContent() {
        return DefaultStreamedContent.builder()
            .name(downloadFileName)
            .contentType("application/octet-stream")
            .stream(() -> safeInputStream(Paths.get(reportFilePathStr)))
            .build();
    }

    private String extractSuffix(String name) {
        return name.substring(name.lastIndexOf("."));
    }

    private Path extractTemplateToTempFile() throws IOException {
        var path = Files.createTempFile("workreporttemplate", templateFile.getFileName());
        Files.copy(templateFile.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        return path;
    }

    public ReportExportParameter getParameter() {
        return parameter;
    }

    public UploadedFile getTemplateFile() {
        return templateFile;
    }

    public void setTemplateFile(UploadedFile templateFile) {
        this.templateFile = templateFile;
    }

}