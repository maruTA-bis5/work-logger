package net.bis5.worklogger.controller;

import javax.enterprise.inject.Model;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.ResourceBundle;

@Model
public class LoginView {

    private final UIViewRoot viewRoot;
    private final FacesContext facesContext;
    private final ExternalContext externalContext;

    @Inject
    public LoginView(UIViewRoot viewRoot, FacesContext facesContext, ExternalContext externalContext) {
        this.viewRoot = viewRoot;
        this.facesContext = facesContext;
        this.externalContext = externalContext;
    }

    private enum Message {
        AUTH_FAIL("authentication-failure");

        private final String code;

        private Message(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        private static Message resolve(String code) {
            return Arrays.stream(values()).filter(m -> m.getCode().equals(code)).findFirst().orElse(null);
        }
    }
    public void processMessage() {
        String messageParam = externalContext.getRequestParameterMap().get("message");
        Message resolvedMessage = Message.resolve(messageParam);
        if (resolvedMessage != null) {
            ResourceBundle bundle = ResourceBundle.getBundle(LoginView.class.getName(), viewRoot.getLocale());
            if (bundle.containsKey(resolvedMessage.getCode())) {
                String messageContent = bundle.getString(resolvedMessage.getCode());
                var message = new FacesMessage(FacesMessage.SEVERITY_ERROR, messageContent, null);
                facesContext.addMessage(null, message);
            }
        }
    }
}
