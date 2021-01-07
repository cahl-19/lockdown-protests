/*
 * This File is Part of LDProtest
 * Copyright (C) 2021 Covid Anti Hysterics League
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Lesser Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package ldprotest.main;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class AppLogging {


    private AppLogging() {
        /* Do not construct */
    }

    public enum ResourceType {
        ON_FILE_SYSTEM,
        ON_CLASSPATH;
    }

    public static final void setLogbackConfig(String path, ResourceType type) throws IOException {

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        JoranConfigurator configurator = new JoranConfigurator();


        InputStream configStream = openResource(path, type);


        configurator.setContext(loggerContext);
        try {
            configurator.doConfigure(configStream);
        }
        catch(JoranException e) {
            throw new IOException("Logback configuration is invalid");
        }
        configStream.close();

    }

    private static InputStream openResource(String path, ResourceType type) throws IOException {
        switch(type) {
            case ON_CLASSPATH:
                return loadResource(path);
            case ON_FILE_SYSTEM:
                return new FileInputStream(path);
            default:
                throw new AssertionError("Invalid enum value.");
        }
    }

    private static InputStream loadResource(String path) throws IOException {
        InputStream ret = AppLogging.class.getResourceAsStream(path);

        if(ret == null) {
            throw new IOException("Resource not found");
        }

        return ret;
    }
}
