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
package ldprotest.server.infra.templates;

import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AliasTemplateLoader implements TemplateLoader {

    private final Map<String, String> aliasMap;
    private final TemplateLoader delegate;

    public AliasTemplateLoader() {
        delegate = new ClassPathTemplateLoader();
        aliasMap = new HashMap<>();
    }

    @Override
    public TemplateSource sourceAt(String location) throws IOException {

        String resolvedAlias = aliasMap.get(location);

        if(resolvedAlias != null) {
            return delegate.sourceAt(resolvedAlias);
        } else {
            return delegate.sourceAt(location);
        }
    }

    @Override
    public String resolve(String location) {
        return delegate.resolve(location);
    }

    @Override
    public String getPrefix() {
        return delegate.getPrefix();
    }

    @Override
    public String getSuffix() {
        return delegate.getSuffix();
    }

    @Override
    public void setPrefix(String prefix) {
        delegate.setPrefix(prefix);
    }

    @Override
    public void setSuffix(String suffix) {
        delegate.setSuffix(suffix);
    }

    public void setAlias(String alias, String resolved) {
        this.aliasMap.put(alias,resolved);
    }
}
