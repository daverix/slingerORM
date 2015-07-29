/*
 * Copyright 2015 David Laurell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.daverix.slingerorm.compiler;

import java.util.ArrayList;
import java.util.List;

final class ListUtils {
    private ListUtils(){}

    public static <T, E> List<T> mapItems(List<E> items, Function<T, E> mapper) {
        List<T> mappedItems = new ArrayList<T>();
        for(E item : items) {
            mappedItems.add(mapper.apply(item));
        }
        return mappedItems;
    }

    public static <T> List<T> filter(List<T> items, Predicate<T> predicate) throws InvalidElementException {
        List<T> filteredItems = new ArrayList<T>();
        for(T item : items) {
            if(predicate.test(item)) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    public static <T> T firstOrDefault(List<T> items) {
        if(items.size() < 1)
            return null;

        return items.get(0);
    }
}
