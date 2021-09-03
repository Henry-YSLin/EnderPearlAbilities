package io.github.henryyslin.enderpearlabilities.utils;

import java.util.List;

public class ListUtils {
    public static <T> T getRandom(List<T> list) {
        int r = (int)Math.floor(Math.random() * list.size());
        return list.get(r);
    }
}
