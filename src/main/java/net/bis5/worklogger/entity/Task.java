package net.bis5.worklogger.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;

@Entity
public class Task extends PanacheEntityBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    public WorkUser user;
    @Column(name = "task_code")
    @NotNull
    public String taskCode;
    @Column(name = "task_name")
    @NotNull
    public String taskName;
    public String description;
    @Column(name = "available_from")
    @NotNull
    public LocalDate availableFrom;
    @Column(name = "available_until")
    @NotNull
    public LocalDate availableUntil;
    @Version
    public Long version;

    public static List<Task> findByUserName(String userName) {
        Optional<WorkUser> user = WorkUser.findByUserName(userName);
        return user.map(u -> Task.<Task>find("user = ?1 order by taskCode", u))
            .map(PanacheQuery::list)
            .orElseGet(List::of);
    }

    public static Optional<Task> findByTaskCode(Long userId, String taskCode) {
        WorkUser user =  WorkUser.findById(userId);
        return Optional.ofNullable(Task.<Task>find("user = :user AND taskCode = :taskCode",
            Parameters.with("user", user).and("taskCode", taskCode)).firstResult());
    }

    public void merge() {
        getEntityManager().merge(this);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Task && this.id == ((Task)obj).id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
