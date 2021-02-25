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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class MultiMap<K, V> {

    private final Map<K, List<V>> backing;
    private final int length = 0;

    public MultiMap() {
        backing = new HashMap<>();
    }

    public int size() {
        return length;
    }

    public boolean isEmpty() {
        return length == 0;
    }

    public boolean containsKey(K arg0) {
        return backing.containsKey(arg0);
    }

    public List<V> get(K key) {
        return Collections.unmodifiableList(backing.get(key));
    }

    public void put(K key, V value) {
        backing.compute(key, (k, v) -> {
            if(v == null) {
                List<V> ret = new ArrayList<>();
                ret.add(value);
                return ret;
            } else {
                v.add(value);
                return v;
            }
        });
    }

    public V remove(K key, V value) {
        AtomicReference<V> ret = new AtomicReference<>();
        backing.compute(key, (k, v) -> {
            if(v == null) {
                return null;
            } else {
                if(v.remove(value)) {
                    ret.set(value);
                }
                return v;
            }
        });
        return ret.get();
    }

    public void putAll(Map<? extends K, ? extends V> arg) {
       for(Entry<? extends K, ? extends V> ent: arg.entrySet()) {
           put(ent.getKey(), ent.getValue());
       }
    }

    public void clear() {
        backing.clear();
    }

    public Set<K> keySet() {
        return backing.keySet();
    }

    public Set<Entry<K, List<V>>> entrySet() {
        return backing.entrySet();
    }

}
