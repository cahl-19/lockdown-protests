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
package ldprotest.business;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import ldprotest.geo.Coordinate;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.serialization.Sanitizable;
import ldprotest.server.infra.http.Sanitize;
import ldprotest.server.infra.http.Validate;
import ldprotest.util.interfaces.Validatable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicProtestData implements JsonSerializable, Validatable, Sanitizable<PublicProtestData> {

    private final static Logger LOGGER = LoggerFactory.getLogger(PublicProtestData.class);

    private static final int MIN_TITLE_LENGTH = 4;
    private static final int MAX_TITLE_LENGTH = 256;
    private static final int MAX_DESCRIPTION_LENGTH = 768;
    private static final int MAX_DRESS_CODE_LENGTH = 128;
    private static final int MAX_HOME_PAGE_LENGTH = 256;

    public final Coordinate location;
    public final Optional<String> owner;

    public final String title;
    public final String description;
    public final Optional<String> dressCode;
    public final Optional<String> homePage;

    public final ZonedDateTime date;

    public final Optional<UUID> protestId;

    @ReflectiveConstructor
    private PublicProtestData() {
        location = null;
        owner = null;

        title = null;
        description = null;
        dressCode = null;
        date = null;
        protestId = null;
        homePage = null;
    }

    private PublicProtestData(
        Coordinate location,
        Optional<String> owner,
        String title,
        String description,
        ZonedDateTime date,
        Optional<String> dressCode,
        Optional<UUID> protestId,
        Optional<String> homePage
    ) {
        this.location = location;
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.dressCode = dressCode;
        this.date = date;
        this.protestId = protestId;
        this.homePage = homePage;
    }

    public PublicProtestData(
        PrivateProtestData data
    ) {
        this(
            data.location,
            data.owner,
            data.title,
            data.description,
            data.date,
            data.dressCode,
            Optional.of(data.protestId),
            data.homePage
        );
    }

    @Override
    public boolean validate() {
        if(title.length() > MAX_TITLE_LENGTH) {
            return false;
        } else if(title.length() < MIN_TITLE_LENGTH) {
            return false;
        } else if(description.length() > MAX_DESCRIPTION_LENGTH) {
            return false;
        } else if(dressCode.isPresent() && dressCode.get().length() > MAX_DRESS_CODE_LENGTH) {
            return false;
        } else if(!location.validate()) {
            return false;
        } else if(homePage.isPresent() && !Validate.validateUrl(homePage.get())) {
            return false;
        } else if(homePage.isPresent() && homePage.get().length() > MAX_HOME_PAGE_LENGTH) {
            return false;
        } else {
            return true;
        }
    }


    @Override
    public PublicProtestData sanitize() {
        return new PublicProtestData(
            location,
            owner,
            Sanitize.encodeHtml(title),
            Sanitize.encodeHtml(description),
            date,
            dressCode.isEmpty() ? dressCode : Optional.of(Sanitize.encodeHtml(dressCode.get())),
            protestId,
            homePage.isEmpty() ? homePage : Sanitize.encodeUrl(homePage.get()).toOpt()
        );
    }
}
