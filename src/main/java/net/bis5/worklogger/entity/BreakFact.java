package net.bis5.worklogger.entity;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
@Table(name = "break_fact")
public class BreakFact extends PanacheEntityBase implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    public WorkUser user;
    @Column(name = "target_date")
    public LocalDate targetDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "break_kind")
    public BreakKind breakKind;
    @Column(name = "break_start_at")
    public ZonedDateTime breakStartAt;
    @Column(name = "break_end_at")
    public ZonedDateTime breakEndAt;
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
        if (breakStartAt == null || breakEndAt == null) {
            totalMins = 0;
        } else {
            Duration breakDuration = Duration.between(breakStartAt, breakEndAt);
            var totalSecs = breakDuration.getSeconds();
            totalMins = (int)totalSecs/60;
        }
    }

    public void merge() {
        preSave();
        getEntityManager().merge(this);
    }

    public static Optional<BreakFact> findInProgressBreak(WorkUser user, LocalDate targetDate, BreakKind kind) {
        return Optional.ofNullable(
            find("user = :user and targetDate = :targetDate and breakKind = :kind and breakEndAt is null",
                Parameters.with("user", user).and("targetDate", targetDate).and("kind", kind)).firstResult()
        );
    }
}
