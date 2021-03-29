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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import ldprotest.util.Result;
import ldprotest.util.types.Pair;

public class StyleCustomizationOptions {

    public static final StyleCustomizationOptions DEFAULT = new StyleCustomizationOptions(
        "", "", "", "", "", "", "", "", "", ""
    );

    public final String ogTitle;
    public final String ogType;
    public final String ogDescription;
    public final String ogUrl;
    public final String ogImage;
    public final String indexTitle;
    public final String bannerHtmlPath;
    public final String cssUrl;
    public final String informationBodyHtmlPath;
    public final String informationTitleHtmlPath;

    public StyleCustomizationOptions(
        String ogTitle,
        String ogType,
        String ogDescription,
        String ogUrl,
        String ogImage,
        String indexTitle,
        String bannerHtmlPath,
        String cssUrl,
        String informationBodyHtmlPath,
        String informationTitleHtmlPath
    ) {
        this.ogTitle = ogTitle;
        this.ogType = ogType;
        this.ogDescription = ogDescription;
        this.ogUrl = ogUrl;
        this.ogImage = ogImage;
        this.indexTitle = indexTitle;
        this.bannerHtmlPath = bannerHtmlPath;
        this.cssUrl = cssUrl;
        this.informationBodyHtmlPath = informationBodyHtmlPath;
        this.informationTitleHtmlPath = informationTitleHtmlPath;
    }

    public Result<FileReadError, String> bannerHtml() {
        return readFile(bannerHtmlPath);
    }

    public Result<FileReadError, String> informationBodyHtml() {
        return readFile(informationBodyHtmlPath);
    }

    public Result<FileReadError, String> informationTitleHtml() {
        return readFile(informationTitleHtmlPath);
    }

    public List<Pair<String, String>> ogMetaProperties() {
        List<Pair<String, String>> ret = new ArrayList<>();

        addMetaPropIfSet(ret, "og:title", ogTitle);
        addMetaPropIfSet(ret, "og:type", ogType);
        addMetaPropIfSet(ret, "og:description", ogDescription);
        addMetaPropIfSet(ret, "og:url", ogUrl);
        addMetaPropIfSet(ret, "og:image", ogImage);

        return ret;
    }

    private static Result<FileReadError, String> readFile(String path) {
        if(path.isEmpty()) {
            return Result.failure(FileReadError.NOT_DEFINED);
        }

        File bannerFile = new File(path);

        if(!bannerFile.exists()) {
            return Result.failure(FileReadError.NO_SUCH_FILE);
        } else  if(!bannerFile.canRead()) {
            return Result.failure(FileReadError.FILE_UNREADABLE);
        } else if(!bannerFile.isFile()) {
            return Result.failure(FileReadError.NOT_A_FILE);
        }

        try(FileInputStream stream = new FileInputStream(bannerFile)) {
            byte[] data = stream.readAllBytes();

            return Result.success(new String(data, Charset.forName("UTF-8")));
        } catch(IOException ex) {
            return Result.failure(FileReadError.IO_ERROR_WHILE_READING);
        }
    }

    private static void addMetaPropIfSet(List<Pair<String, String>> props, String property, String content) {
        if(content != null && !content.isEmpty()) {
            props.add(new Pair<String, String>(property, content));
        }
    }

    public enum FileReadError {
        NOT_DEFINED("Banner file is not defined"),
        NO_SUCH_FILE("File does not exist"),
        FILE_UNREADABLE("File is not readable"),
        NOT_A_FILE("Path does not point to a file"),
        IO_ERROR_WHILE_READING("Encountered an IO Error while attempting to read the file");

        public final String explanation;

        private FileReadError(String explanation) {
            this.explanation = explanation;
        }
    }
}
