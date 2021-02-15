package net.bis5.worklogger.entity;

import java.io.Serializable;
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

@Entity
@Table(name = "mattermost_config")
public class MattermostConfig extends PanacheEntityBase implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    public WorkUser user;

    @Column(name = "mattermost_url")
    public String mattermostUrl;

    @Column(name = "mattermost_username")
    public String mattermostUsername;

    @Column(name = "mattermost_password")
    public String mattermostPassword;

    @Column(name = "team_id")
    public String teamId;

    @Column(name = "channel_id")
    public String channelId;

    @Version
    public Long version;

    public static Optional<MattermostConfig> findByUser(WorkUser user) {
        return find("user", user).firstResultOptional();
    }

    public void merge() {
        getEntityManager().merge(this);
    }
}
