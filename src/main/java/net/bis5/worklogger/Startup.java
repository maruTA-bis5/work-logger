package net.bis5.worklogger;

import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;
import javax.transaction.Transactional;

import io.quarkus.runtime.StartupEvent;
import net.bis5.worklogger.entity.WorkUser;

@Singleton
public class Startup {

    @Transactional
    public void initialize(@Observes StartupEvent startup) {
        if (WorkUser.findByUserName("root").isEmpty()) {
            WorkUser.add("root", "p@ssw0rd", "admin");
            Logger.getLogger(Startup.class.getName()).info("root user added.");
        }
    }
}
