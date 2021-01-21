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
package ldprotest.util.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UrlPathPrefixTree<R> {

    private final PrefixTree<String, R> delegate;

    public UrlPathPrefixTree() {
        this.delegate = new PrefixTree<>();
    }

    public void add(String path, R data) {
        add(splitPath(path), data);
    }

    public void add(Iterable<String> path, R data) {
        delegate.add(path, data);
    }

    public Optional<R> lookup(String path) {
        return lookup(splitPath(path));
    }

    public Optional<R> lookup(Iterable<String> path) {
        return delegate.lookup(path);
    }

    public Optional<R> longest(String path) {
        return longest(splitPath(path));
    }

    public Optional<R> longest(Iterable<String> path) {
        return delegate.longest(path);
    }

    public static List<String> splitPath(String path) {

        List<String> elems = new ArrayList<>();
        int len = path.length();

        int startOfField = 0;

        for(int i = 0; i < len; i++) {
            if(path.charAt(i) == '/') {
                if(i == 0) {
                    elems.add("/");
                } else {
                    elems.add(path.substring(startOfField, i));
                }
                startOfField = i + 1;
            }
        }

        if(!path.equals("/")) {
            elems.add(path.substring(startOfField));
        }

        return elems;
    }
}
