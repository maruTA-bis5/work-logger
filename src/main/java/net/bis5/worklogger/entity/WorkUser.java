package net.bis5.worklogger.entity;

import java.io.Serializable;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;

@Entity
@Table(name = "work_user")
@UserDefinition
public class WorkUser extends PanacheEntityBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Username
    @Column(name = "user_name")
    public String userName;
    @Password
    public String password;
    @Roles
    public String role;
    @Version
    public Long version;

    public static Optional<WorkUser> findByUserName(String userName) {
        return Optional.ofNullable(find("userName", userName).firstResult());
    }

    public static void add(String userName, String rawPassword, String role) {
        WorkUser user = new WorkUser();
        user.userName = userName;
        user.password = BcryptUtil.bcryptHash(rawPassword);
        user.role = role;
        user.persist();
    }

    public void merge() {
        getEntityManager().merge(this);
    }

	public void updatePassword(String current, String newPassword) {
        // TODO verify current password
        this.password = BcryptUtil.bcryptHash(newPassword);
        merge();
	}
}
