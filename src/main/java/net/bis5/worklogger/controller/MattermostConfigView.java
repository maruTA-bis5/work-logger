package net.bis5.worklogger.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import net.bis5.mattermost.client4.ApiResponse;
import net.bis5.mattermost.client4.MattermostClient;
import net.bis5.mattermost.model.User;
import net.bis5.worklogger.entity.MattermostConfig;
import net.bis5.worklogger.entity.WorkUser;

@ViewScoped
@Named
public class MattermostConfigView implements Serializable {

    private MattermostConfig config;

    public MattermostConfig getConfig() {
        return config;
    }

    private final WorkUser user;

    @Inject
    public MattermostConfigView(HttpServletRequest request) {
        user = WorkUser.findByUserName(request.getRemoteUser()).get();
    }

    public void loadCurrent() {
        config = MattermostConfig.findByUser(user).orElseGet(MattermostConfig::new);
        if (config.mattermostUrl != null && config.mattermostUsername != null && config.mattermostPassword != null) {
            testConnection();
            if (config.teamId != null) {
                onTeamChanged();
            }
        }
    }

    public void testConnection() {
        try (var client = new MattermostClient(config.mattermostUrl)) {
            ApiResponse<User> loginResult = client.login(config.mattermostUsername, config.mattermostPassword);
            if (loginResult.hasError()) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to login", null));
                return;
            }
            User mmUser = loginResult.readEntity();
            this.teams = client.getTeamsForUser(mmUser.getId()).readEntity().stream()
                .map(t -> new SelectItem(t.getId(), t.getDisplayName()))
                .collect(Collectors.toList());
            client.logout();
        } catch (RuntimeException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to login", e.getMessage()));
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private List<SelectItem> teams = new ArrayList<>();

    public List<SelectItem> getTeams() {
        return teams;
    }

    public void onTeamChanged() {
        try (var client = new MattermostClient(config.mattermostUrl)) {
            ApiResponse<User> loginResult = client.login(config.mattermostUsername, config.mattermostPassword);
            if (loginResult.hasError()) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to login", null));
                return;
            }
            User mmUser = loginResult.readEntity();
            channels = client.getChannelsForTeamForUser(config.teamId, mmUser.getId()).readEntity().stream()
                .map(c -> new SelectItem(c.getId(), c.getDisplayName()))
                .collect(Collectors.toList());
            client.logout();
        } catch (RuntimeException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private List<SelectItem> channels = new ArrayList<>();

    public List<SelectItem> getChannels() {
        return channels;
    }

    @Transactional
    public void save() {
        if (config.id != null) {
            config.merge();
        } else {
            config.user = user;
            config.persist();
        }
    }

}
