package io.github.henry_yslin.enderpearlabilities.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class StringUtils {
    /**
     * A safe version of {@link String}{@code .substring}, which does not throw exceptions in edge cases.
     *
     * @param str    A string.
     * @param start  The 0-based beginning index, inclusive.
     * @param length The (maximum) length of the substring.
     * @return The specified substring.
     */
    @Contract(value = "!null, _, _ -> !null; null, _, _ -> null", pure = true)
    public static String substring(@Nullable String str, @Range(from = Integer.MIN_VALUE, to = Integer.MAX_VALUE) int start, @Range(from = Integer.MIN_VALUE, to = Integer.MAX_VALUE) int length) {
        if (str == null) return null;
        if (length < 0) return "";
        return str.substring(Math.max(0, start), Math.min(str.length(), start + length));
    }
}
