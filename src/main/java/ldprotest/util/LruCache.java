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
package ldprotest.util;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class LruCache<S, K, T> {

    private final TreeSet<CacheEntry> cacheTree;
    private final Map<K, CacheEntry> lookupMap;
    private final BiFunction<T, S, S> magOnInsert;
    private final BiFunction<T, S, S> magOnRemove;
    private final Function<S, Boolean> shouldEvict;
    private S magnitude;

    public LruCache(
        BiFunction<T, S, S> magOnInsert,
        BiFunction<T, S, S> magOnRemove,
        Function<S, Boolean> shouldEvict,
        S magInitial
    ) {
        this.cacheTree = new TreeSet<>();
        this.lookupMap = new HashMap<>();
        this.magOnInsert = magOnInsert;
        this.magOnRemove = magOnRemove;
        this.shouldEvict = shouldEvict;

        this.magnitude  = magInitial;
    }

    public synchronized void insert(K key, T elem) {

        CacheEntry entry = new CacheEntry(elem, key);
        CacheEntry prev = lookupMap.put(key, entry);

        cacheTree.add(entry);

        if(prev != null) {
            magnitude = magOnRemove.apply(prev.value, magnitude);
            cacheTree.remove(prev);
        }
        magnitude = magOnInsert.apply(elem, magnitude);

        if(shouldEvict.apply(magnitude)) {
            evict();
        }
    }

    public synchronized T get(K key) {
        CacheEntry entry = lookupMap.get(key);
        if(entry == null) {
            return null;
        } else {
            cacheTree.remove(entry);
            cacheTree.add(new CacheEntry(entry.value, entry.key));

            return entry.value;
        }
    }

    public synchronized T computeIfAbsent(K key, Supplier<T> supplier) {
        T original = get(key);

        if(original == null) {
            T newValue = supplier.get();
            insert(key, newValue);
            return newValue;
        } else {
            return original;
        }
    }


    private void evict() {
        CacheEntry evicted = cacheTree.first();
        cacheTree.remove(evicted);
        lookupMap.remove(evicted.key);

        magnitude = magOnRemove.apply(evicted.value, magnitude);
    }

    private final class CacheEntry implements Comparable<CacheEntry> {

        private final long timestamp;
        public final T value;
        public final K key;

        CacheEntry(T value, K key) {
            this.timestamp = System.currentTimeMillis();
            this.value = value;
            this.key = key;
        }

        @Override
        public int compareTo(CacheEntry cacheEntry) {
            if(cacheEntry.timestamp == this.timestamp) {
                return 0;
            } else {
                return cacheEntry.timestamp > this.timestamp ? 1 : -1;
            }
        }
    }
}
