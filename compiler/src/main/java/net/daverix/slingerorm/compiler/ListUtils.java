package net.daverix.slingerorm.compiler;

import java.util.ArrayList;
import java.util.List;

public final class ListUtils {
    private ListUtils(){}

    public static <T, E> List<T> mapItems(List<E> items, Function<T, E> mapper) {
        List<T> mappedItems = new ArrayList<T>();
        for(E item : items) {
            mappedItems.add(mapper.apply(item));
        }
        return mappedItems;
    }

    public static <T> List<T> filter(List<T> items, Predicate<T> predicate) {
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
