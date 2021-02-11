package net.bis5.worklogger.controller.top;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import net.bis5.worklogger.controller.top.WorkFinishEventConsumer.WorkFinishEvent;
import net.bis5.worklogger.entity.AttendanceFact;
import net.bis5.worklogger.entity.BreakFact;
import net.bis5.worklogger.entity.BreakKind;
import net.bis5.worklogger.entity.WorkUser;

@Named
@ViewScoped
public class AttendanceController implements Serializable {

    private final HttpServletRequest request;
    private final Event<WorkFinishEvent> workFinishEventBus;

    @Inject
    public AttendanceController(HttpServletRequest request, Event<WorkFinishEvent> workFinishEventBus) {
        this.request = request;
        this.workFinishEventBus = workFinishEventBus;
    }

    private WorkUser user;

    @PostConstruct
    void initialize() {
        user = WorkUser.findByUserName(request.getRemoteUser()).get();
    }

    @Transactional
    public void onWorkStart() {
        var attendance = new AttendanceFact();
        attendance.user = user;
        attendance.targetDate = LocalDate.now();
        attendance.workStartAt = ZonedDateTime.now();
        attendance.persist();
    }

    public boolean isCanWorkStart() {
        return !isWorkStarted();
    }

    @Transactional
    public void onWorkEnd() {
        onBreakEnd();
        onLunchEnd();
        onPrivateOutEnd();
        fireWorkFinishEvent();
        findAttendanceFact().ifPresent(a -> {
            a.workEndAt = ZonedDateTime.now();
            a.merge();
        });
    }

    private boolean isWorkStarted() {
        return findAttendanceFact().isPresent();
    }
    private Optional<AttendanceFact> findAttendanceFact() {
        return AttendanceFact.findByTargetDate(user, LocalDate.now());
    }

    public boolean isCanWorkEnd() {
        return isWorkStarted();
    }

    @Transactional
    public void onBreakStart() {
        onBreakStart(BreakKind.BREAK);
    }

    private void onBreakStart(BreakKind kind) {
        fireWorkFinishEvent();
        Arrays.stream(BreakKind.values())
            .filter(k -> k != kind)
            .map(this::findInProgressBreak)
            .forEach(b -> b.ifPresent(this::endBreak));

        var breakFact = new BreakFact();
        breakFact.user = user;
        breakFact.breakKind = kind;
        breakFact.targetDate = LocalDate.now();
        breakFact.breakStartAt = ZonedDateTime.now();
        breakFact.persist();
    }

    public boolean isCanBreakStart() {
        return isCanBreakStart(BreakKind.BREAK);
    }

    private boolean isCanBreakStart(BreakKind breakKind) {
        return findAttendanceFact().isPresent() && findInProgressBreak(breakKind).isEmpty();
    }

    private Optional<BreakFact> findInProgressBreak(BreakKind breakKind) {
        return BreakFact.findInProgressBreak(user, LocalDate.now(), breakKind);
    }

    @Transactional
    public void onBreakEnd() {
        onBreakEnd(BreakKind.BREAK);
    }

    private void onBreakEnd(BreakKind kind) {
        findInProgressBreak(kind).ifPresent(this::endBreak);
    }

    private void endBreak(BreakFact breakFact) {
        breakFact.breakEndAt = ZonedDateTime.now();
        breakFact.merge();
    }

    public boolean isCanBreakEnd() {
        return findInProgressBreak(BreakKind.BREAK).isPresent();
    }

    @Transactional
    public void onLunchStart() {
        onBreakStart(BreakKind.LUNCH);
    }

    public boolean isCanLunchStart() {
        return findAttendanceFact().isPresent() && findInProgressBreak(BreakKind.LUNCH).isEmpty();
    }

    @Transactional
    public void onLunchEnd() {
        onBreakEnd(BreakKind.LUNCH);
    }

    public boolean isCanLunchEnd() {
        return findInProgressBreak(BreakKind.LUNCH).isPresent();
    }

    @Transactional
    public void onPrivateOutStart() {
        onBreakStart(BreakKind.PRIVATE_OUT);
    }

    public boolean isCanPrivateOutStart() {
        return findAttendanceFact().isPresent() && findInProgressBreak(BreakKind.PRIVATE_OUT).isEmpty();
    }

    @Transactional
    public void onPrivateOutEnd() {
        onBreakEnd(BreakKind.PRIVATE_OUT);
    }

    public boolean isCanPrivateOutEnd() {
        return findInProgressBreak(BreakKind.PRIVATE_OUT).isPresent();
    }

    private void fireWorkFinishEvent() {
        workFinishEventBus.fire(new WorkFinishEvent(user));
    }
}
