package net.bis5.worklogger.controller.top;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.enterprise.event.Event;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import net.bis5.worklogger.controller.top.WorkFinishEventConsumer.WorkFinishEvent;
import net.bis5.worklogger.entity.AttendanceFact;
import net.bis5.worklogger.entity.BreakFact;
import net.bis5.worklogger.entity.BreakKind;
import net.bis5.worklogger.service.MattermostNotifyEvent;

@QuarkusTest
class AttendanceControllerTest {

    private HttpServletRequest request;

    private AttendanceController controller;

    abstract class Base {

        @BeforeEach
        void setUp() {
            request = mock(HttpServletRequest.class);
            when(request.getRemoteUser()).thenReturn("user");
            Event<WorkFinishEvent> workFinishEventBus = mock(Event.class);
            Event<MattermostNotifyEvent> notifyEventBus = mock(Event.class);

            controller = new AttendanceController(request, workFinishEventBus, notifyEventBus);
        }

        @AfterEach
        void tearDown() {
            PanacheMock.reset();
        }

    }

    @Nested
    class TestIsCanWorkStart extends Base {

        @Test
        void 出勤済なら出勤打刻不可() {
            PanacheMock.mock(AttendanceFact.class);
            var attendance = new AttendanceFact();
            attendance.workStartAt = ZonedDateTime.now();
            PanacheMock.doReturn(Optional.of(attendance)).when(AttendanceFact.class).findByTargetDate(any(), any());

            assertFalse(controller.isCanWorkStart());
        }

        @Test
        void 出勤していなければ打刻可能() {
            PanacheMock.mock(AttendanceFact.class);
            PanacheMock.doReturn(Optional.empty()).when(AttendanceFact.class).findByTargetDate(any(), any());

            assertTrue(controller.isCanWorkStart());
        }
    }

    @Nested
    class TestIsCanWorkEnd extends Base {

        @Test
        void 出勤済みなら退勤打刻可能() {
            PanacheMock.mock(AttendanceFact.class);
            var attendance = new AttendanceFact();
            attendance.workStartAt = ZonedDateTime.now();
            PanacheMock.doReturn(Optional.of(attendance)).when(AttendanceFact.class).findByTargetDate(any(), any());

            assertTrue(controller.isCanWorkEnd());
        }

        @Test
        void 出勤済みなら退勤打刻可能_退勤済みでも可能() {
            PanacheMock.mock(AttendanceFact.class);
            var attendance = new AttendanceFact();
            attendance.workStartAt = ZonedDateTime.now();
            attendance.workEndAt = ZonedDateTime.now().plusHours(8);
            PanacheMock.doReturn(Optional.of(attendance)).when(AttendanceFact.class).findByTargetDate(any(), any());

            assertTrue(controller.isCanWorkEnd());
        }

        @Test
        void 出勤していなければ退勤打刻不可() {
            PanacheMock.mock(AttendanceFact.class);
            PanacheMock.doReturn(Optional.empty()).when(AttendanceFact.class).findByTargetDate(any(), any());

            assertFalse(controller.isCanWorkEnd());
        }
    }

    @Nested
    class TestIsCanBreakStart extends Base {

        @Test
        void 休憩中なら休憩開始打刻不可() {
            PanacheMock.mock(BreakFact.class);
            var breakFact = new BreakFact();
            breakFact.breakStartAt = ZonedDateTime.now();
            PanacheMock.doReturn(Optional.of(breakFact)).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.BREAK));

            assertFalse(controller.isCanBreakStart());
        }

        @Test
        void 出勤済みで休憩中でなければ休憩開始打刻可能() {
            PanacheMock.mock(AttendanceFact.class);
            var attendance = new AttendanceFact();
            attendance.workStartAt = ZonedDateTime.now();
            PanacheMock.doReturn(Optional.of(attendance)).when(AttendanceFact.class).findByTargetDate(any(), any());
            PanacheMock.mock(BreakFact.class);
            PanacheMock.doReturn(Optional.empty()).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.BREAK));

            assertTrue(controller.isCanBreakStart());
        }

        @Test
        void 出勤していなければ休憩開始打刻不可() {
            PanacheMock.mock(AttendanceFact.class);
            PanacheMock.doReturn(Optional.empty()).when(AttendanceFact.class).findByTargetDate(any(), any());
            PanacheMock.mock(BreakFact.class);
            PanacheMock.doReturn(Optional.empty()).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.BREAK));

            assertFalse(controller.isCanBreakStart());
        }

    }

    @Nested
    class TestIsCanBreakEnd extends Base {

        @Test
        void 休憩中なら休憩終了打刻可能() {
            PanacheMock.mock(BreakFact.class);
            var breakFact = new BreakFact();
            breakFact.breakStartAt = ZonedDateTime.now();
            PanacheMock.doReturn(Optional.of(breakFact)).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.BREAK));

            assertTrue(controller.isCanBreakEnd());
        }

        @Test
        void 休憩中でなければ休憩終了打刻不可() {
            PanacheMock.mock(BreakFact.class);
            PanacheMock.doReturn(Optional.empty()).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.BREAK));

            assertFalse(controller.isCanBreakEnd());
        }
    }

    @Nested
    class TestIsCanLunchStart extends Base {

        @Test
        void 昼休み中なら昼休み開始打刻不可() {
            PanacheMock.mock(BreakFact.class);
            var breakFact = new BreakFact();
            breakFact.breakStartAt = ZonedDateTime.now();
            PanacheMock.doReturn(Optional.of(breakFact)).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.LUNCH));

            assertFalse(controller.isCanLunchStart());
        }

        @Test
        void 出勤済みで昼休み中でなければ昼休み開始打刻可能() {
            PanacheMock.mock(AttendanceFact.class);
            var attendance = new AttendanceFact();
            attendance.workStartAt = ZonedDateTime.now();
            PanacheMock.doReturn(Optional.of(attendance)).when(AttendanceFact.class).findByTargetDate(any(), any());
            PanacheMock.mock(BreakFact.class);
            PanacheMock.doReturn(Optional.empty()).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.LUNCH));

            assertTrue(controller.isCanLunchStart());
        }

        @Test
        void 出勤していなければ昼休み開始打刻不可() {
            PanacheMock.mock(AttendanceFact.class);
            PanacheMock.doReturn(Optional.empty()).when(AttendanceFact.class).findByTargetDate(any(), any());
            PanacheMock.mock(BreakFact.class);
            PanacheMock.doReturn(Optional.empty()).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.LUNCH));

            assertFalse(controller.isCanLunchStart());
        }

    }

    @Nested
    class TestIsCanLunchEnd extends Base {

        @Test
        void 昼休み中なら昼休み終了打刻可能() {
            PanacheMock.mock(BreakFact.class);
            var breakFact = new BreakFact();
            breakFact.breakStartAt = ZonedDateTime.now();
            PanacheMock.doReturn(Optional.of(breakFact)).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.LUNCH));

            assertTrue(controller.isCanLunchEnd());
        }

        @Test
        void 昼休み中でなければ昼休み終了打刻不可() {
            PanacheMock.mock(BreakFact.class);
            PanacheMock.doReturn(Optional.empty()).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.LUNCH));

            assertFalse(controller.isCanLunchEnd());
        }
    }

    @Nested
    class TestIsCanPrivateOutStart extends Base {

        @Test
        void 私用外出中なら私用外出開始打刻不可() {
            PanacheMock.mock(BreakFact.class);
            var breakFact = new BreakFact();
            breakFact.breakStartAt = ZonedDateTime.now();
            PanacheMock.doReturn(Optional.of(breakFact)).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.PRIVATE_OUT));

            assertFalse(controller.isCanPrivateOutStart());
        }

        @Test
        void 出勤済みで私用外出中でなければ私用外出開始打刻可能() {
            PanacheMock.mock(AttendanceFact.class);
            var attendance = new AttendanceFact();
            attendance.workStartAt = ZonedDateTime.now();
            PanacheMock.doReturn(Optional.of(attendance)).when(AttendanceFact.class).findByTargetDate(any(), any());
            PanacheMock.mock(BreakFact.class);
            PanacheMock.doReturn(Optional.empty()).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.PRIVATE_OUT));

            assertTrue(controller.isCanPrivateOutStart());
        }

        @Test
        void 出勤していなければ私用外出開始打刻不可() {
            PanacheMock.mock(AttendanceFact.class);
            PanacheMock.doReturn(Optional.empty()).when(AttendanceFact.class).findByTargetDate(any(), any());
            PanacheMock.mock(BreakFact.class);
            PanacheMock.doReturn(Optional.empty()).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.PRIVATE_OUT));

            assertFalse(controller.isCanPrivateOutStart());
        }

    }

    @Nested
    class TestIsCanPrivateOutEnd extends Base {

        @Test
        void 私用外出中なら私用外出終了打刻可能() {
            PanacheMock.mock(BreakFact.class);
            var breakFact = new BreakFact();
            breakFact.breakStartAt = ZonedDateTime.now();
            PanacheMock.doReturn(Optional.of(breakFact)).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.PRIVATE_OUT));

            assertTrue(controller.isCanPrivateOutEnd());
        }

        @Test
        void 私用外出中でなければ私用外出終了打刻不可() {
            PanacheMock.mock(BreakFact.class);
            PanacheMock.doReturn(Optional.empty()).when(BreakFact.class).findInProgressBreak(any(), any(), eq(BreakKind.PRIVATE_OUT));

            assertFalse(controller.isCanPrivateOutEnd());
        }
    }

}
