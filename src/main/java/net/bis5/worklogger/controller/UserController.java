package net.bis5.worklogger.controller;

import java.io.IOException;

import javax.enterprise.inject.Model;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.primefaces.PrimeFaces;

import net.bis5.worklogger.entity.WorkUser;

@Model
public class UserController {

    private final WorkUser user;
    private final HttpServletRequest request;

    @Inject
    public UserController(HttpServletRequest request) {
        this.request = request;
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

    public String logout() {
        var session = (HttpSession)FacesContext.getCurrentInstance().getExternalContext().getSession(true);
        session.invalidate();
        FacesContext.getCurrentInstance().getExternalContext().addResponseCookie("JSESSIONID", null, null);
        return "login.xhtml?faces-redirect=true";
    }
}
