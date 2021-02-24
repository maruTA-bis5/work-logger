package net.bis5.worklogger.controller;

import javax.enterprise.inject.Model;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.primefaces.PrimeFaces;

import io.quarkus.security.identity.SecurityIdentity;
import net.bis5.worklogger.entity.WorkUser;

@Model
public class UserController {

    private final WorkUser user;

    @Inject
    public UserController(HttpServletRequest request) {
        user = WorkUser.findByUserName(request.getRemoteUser()).get();
    }

    public void changePassword() {
        PrimeFaces.current().dialog().openDynamic("changePassword");
    }

    private ChangePassword changePassword = new ChangePassword();

    public ChangePassword getChangePassword() {
        return changePassword;
    }

    public class ChangePassword {
        private String current;
        private String newPassword;
        public void setCurrent(String current) {this.current = current;}
        public void setNewPassword(String newPassword) {this.newPassword = newPassword;}
        public String getCurrent() { return current; }
        public String getNewPassword() { return newPassword; }
        public void apply() {
            user.updatePassword(current, newPassword);
        }
    }

    @Transactional
    public void applyChangePassword() {
        changePassword.apply();
        PrimeFaces.current().dialog().closeDynamic(null);
    }
}
