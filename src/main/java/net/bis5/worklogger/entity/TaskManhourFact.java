package net.bis5.worklogger.entity;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;

@Entity
@Table(name = "task_manhour_fact")
public class TaskManhourFact extends PanacheEntityBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", referencedColumnName = "id")
    public Task task;
    @Column(name = "target_date")
    public LocalDate targetDate;
    @Column(name = "work_start_at")
    public ZonedDateTime workStartAt;
    @Column(name = "work_end_at")
    public ZonedDateTime workEndAt;
    @Column(name = "total_mins")
    public int totalMins;
    @Version
    public Long version;

    @Override
    public void persist() {
        preSave();
        super.persist();
    }

    private void preSave() {
        if (workStartAt == null || workEndAt == null) {
            totalMins = 0;
        } else {
            Duration workDuration = Duration.between(workStartAt, workEndAt);
            var totalSecs = workDuration.getSeconds();
            totalMins = (int)totalSecs/60;
        }
    }

    public void merge() {
        preSave();
        getEntityManager().merge(this);
    }

    public static List<TaskManhourFact> findByTask(Task task) {
        return find("task", task).list();
    }

    public static Optional<TaskManhourFact> findCurrentTask(WorkUser user) {
        return Optional.ofNullable(
            find("from TaskManhourFact f inner join fetch f.task t where t.user = :user and f.workEndAt is null",
            Parameters.with("user", user)).firstResult()
        );
    }

    public static List<TaskManhourFact> findByTargetDate(WorkUser user, LocalDate targetDate) {
        return find("from TaskManhourFact f inner join fetch f.task t where t.user = :user and f.targetDate = :targetDate",
            Parameters.with("user", user).and("targetDate", targetDate))
            .list();
    }

    public static List<TaskManhourFact> findByYearMonth(WorkUser user, YearMonth targetMonth) {
        return find("from TaskManhourFact f inner join fetch f.task t where t.user = :user AND f.targetDate BETWEEN :from AND :to", 
            Parameters.with("user", user)
                .and("from", targetMonth.atDay(1))
                .and("to", targetMonth.atEndOfMonth())
            ).list();
    }

}
