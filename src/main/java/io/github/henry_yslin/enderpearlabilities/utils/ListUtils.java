package io.github.henry_yslin.enderpearlabilities.utils;

import java.util.List;

public class ListUtils {

    /**
     * Get a random element from a list.
     *
     * @param list The list to get a random element from.
     * @return A random element of the list.
     */
    public static <T> T getRandom(List<T> list) {
        int r = (int) Math.floor(Math.random() * list.size());
        return list.get(r);
    }
}
