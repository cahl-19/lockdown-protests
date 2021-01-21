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

import java.util.Arrays;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class UnitTestUrlPathPrefixTree {

    @Test
    public void testSplit() {
        assertEquals(Arrays.asList("/"), UrlPathPrefixTree.splitPath("/"));
        assertEquals(Arrays.asList("/", "", ""), UrlPathPrefixTree.splitPath("//"));
        assertEquals(Arrays.asList("/", "", "", ""), UrlPathPrefixTree.splitPath("///"));
        assertEquals(Arrays.asList("a", "b", ""), UrlPathPrefixTree.splitPath("a/b/"));
        assertEquals(Arrays.asList("/", "a", "b", "", ""), UrlPathPrefixTree.splitPath("/a/b//"));
    }

    @Test
    public void testExact() {
        UrlPathPrefixTree<String> tree = new UrlPathPrefixTree<>();

        tree.add("/", "data-1");

        assertEquals(Optional.of("data-1"), tree.lookup("/"));
        assertEquals(Optional.of("data-1"), tree.longest("/"));
    }

    @Test
    public void testLonger() {
        UrlPathPrefixTree<String> tree = new UrlPathPrefixTree<>();

        tree.add("/", "data-1");
        tree.add("/a", "data-2");

        assertEquals(Optional.of("data-2"), tree.lookup("/a"));
        assertEquals(Optional.of("data-2"), tree.longest("/a"));

        assertEquals(Optional.of("data-1"), tree.lookup("/"));
        assertEquals(Optional.of("data-1"), tree.longest("/"));
    }

    @Test
    public void noExact() {
        UrlPathPrefixTree<String> tree = new UrlPathPrefixTree<>();

        tree.add("/", "data-1");
        tree.add("/a", "data-2");

        assertEquals(Optional.empty(), tree.lookup("/a/b"));
        assertEquals(Optional.of("data-2"), tree.longest("/a/b"));
    }

    @Test
    public void branches() {
        UrlPathPrefixTree<String> tree = new UrlPathPrefixTree<>();

        tree.add("/", "data-1");

        tree.add("/a", "data-2");
        tree.add("/a/b", "data-3");

        tree.add("/1", "data-4");
        tree.add("/1/2", "data-5");
        tree.add("/1/2/3", "data-6");

        tree.add("/1/a/b", "data-7");

        assertEquals(Optional.of("data-1"), tree.lookup("/"));
        assertEquals(Optional.of("data-1"), tree.longest("/"));

        assertEquals(Optional.of("data-2"), tree.longest("/a"));
        assertEquals(Optional.of("data-2"), tree.lookup("/a"));

        assertEquals(Optional.of("data-3"), tree.longest("/a/b"));
        assertEquals(Optional.of("data-3"), tree.lookup("/a/b"));

        assertEquals(Optional.of("data-4"), tree.longest("/1"));
        assertEquals(Optional.of("data-4"), tree.lookup("/1"));

        assertEquals(Optional.of("data-5"), tree.longest("/1/2"));
        assertEquals(Optional.of("data-5"), tree.lookup("/1/2"));

        assertEquals(Optional.of("data-6"), tree.longest("/1/2/3"));
        assertEquals(Optional.of("data-6"), tree.lookup("/1/2/3"));

        assertEquals(Optional.of("data-4"), tree.longest("/1/a"));
        assertEquals(Optional.empty(), tree.lookup("/1/a"));

        assertEquals(Optional.of("data-7"), tree.longest("/1/a/b"));
        assertEquals(Optional.of("data-7"), tree.lookup("/1/a/b"));

        assertEquals(Optional.of("data-1"), tree.longest("/does/not/exist"));
        assertEquals(Optional.empty(), tree.lookup("/does/not/exist"));

        assertEquals(Optional.empty(), tree.longest("does/not/exist"));
        assertEquals(Optional.empty(), tree.lookup("does/not/exist"));
    }
}
