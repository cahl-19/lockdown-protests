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
package ldprotest.config;

import java.io.InputStream;
import ldprotest.util.Result;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class ConfigFile {

    private ConfigFile() {
        /* do not construct */
    }

    public static Result<String, AppConfig.Builder> parse(AppConfig.Builder builder, InputStream file) {

        Yaml yaml = new Yaml(new Constructor(ConfigFileData.class));
        ConfigFileData data = yaml.load(file);

        if(data.usingHttps != null) {
            builder.setUsingHttps(data.usingHttps, AppConfig.PRIORITY_CONFIG);
        }

        return Result.success(builder);
    }

    private static final class ConfigFileData {
        public Boolean usingHttps;
    }
}
