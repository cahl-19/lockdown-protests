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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import ldprotest.util.IterTools;

public class PrefixTree<T, R> {

    private final TreeNode<T, R> root;

    public PrefixTree() {
        root = new TreeNode<>();
    }

    public synchronized void add(Iterable<T> path, R data) {
        IterTools.iterateUntilLast(
            root, path,
            (node, elem) -> node.link(elem),
            (node, elem) -> node.link(elem, data)
        );
    }

    public synchronized Optional<R> lookup(Iterable<T> path) {
        TreeNode<T, R> current = root;

        if(current == null) {
            return Optional.empty();
        }

        for(T elem: path) {
            current = current.lookup(elem);

            if(current == null) {
                return Optional.empty();
            }
        }

        return current.data();
    }

    public synchronized Optional<R> longest(Iterable<T> path) {

        TreeNode<T, R> current = root;
        TreeNode<T, R> longest = root;

        if(current == null) {
            return Optional.empty();
        }

        for(T elem: path) {
            current = current.lookup(elem);

            if(current == null) {
                return longest.data();
            } else if(current.data.isPresent()) {
                longest = current;
            }
        }

        return longest.data();
    }

    private static final class TreeNode<T, R> {

        private final Map<T, TreeNode> children;
        private final Optional<R> data;

        private TreeNode(Map<T, TreeNode> children, Optional<R> data) {
            this.children = children;
            this.data = data;
        }

        public TreeNode() {
            this(new HashMap<>(), Optional.empty());
        }

        public TreeNode(R data) {
            this(new HashMap<>(), Optional.of(data));
        }

        public Optional<R> data() {
            return this.data;
        }

        @SuppressWarnings("unchecked")
        public TreeNode<T, R> link(T key) {
            return children.computeIfAbsent(key, (k) -> new TreeNode<>());
        }

        @SuppressWarnings("unchecked")
        public TreeNode<T, R> link(T key, R data) {
            return children.compute(key, (k, v) -> {
                if(v == null) {
                    return new TreeNode<>(data);
                } else {
                    return new TreeNode<>(v.children, Optional.of(data));
                }
            });
        }

        @SuppressWarnings("unchecked")
        public TreeNode<T, R> lookup(T key) {
            return children.get(key);
        }
    }
}
