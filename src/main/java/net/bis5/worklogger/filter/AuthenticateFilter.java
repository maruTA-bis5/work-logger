package net.bis5.worklogger.filter;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebFilter("/*")
public class AuthenticateFilter implements Filter {

    private final Set<String> authNotRequiredPaths = Set.of("/login.xhtml", "/error.xhtml");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var httpRequest = (HttpServletRequest)request;
        boolean faces = httpRequest.getRequestURI().endsWith(".xhtml");
        boolean facesResource = faces && httpRequest.getRequestURI().contains("javax.faces.resource");
        boolean authenticated = httpRequest.getRemoteUser() != null;
        if (faces && !facesResource && !authenticated && !authNotRequiredPaths.contains(httpRequest.getRequestURI())) {
            var httpResponse = (HttpServletResponse)response;
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setHeader("Location", "/login.xhtml");
            Logger.getLogger(getClass().getName()).info("redirected!!!");
        } else {
            chain.doFilter(request, response);
        }
    }

    
}
