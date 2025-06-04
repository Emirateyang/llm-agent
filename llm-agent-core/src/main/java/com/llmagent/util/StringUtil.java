/*
 *  Copyright (c) 2023-2025, llm-agent (emirate.yang@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.llmagent.util;

public class StringUtil {
    public static boolean noText(String string) {
        return !hasText(string);
    }

    public static boolean hasText(String string) {
        return string != null && !string.isEmpty() && containsText(string);
    }

    private static boolean containsText(CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String obtainFirstHasText(String... strings) {
        for (String string : strings) {
            if (hasText(string)) {
                return string;
            }
        }
        return null;
    }

    public static String quoted(Object object) {
        if (object == null) {
            return "null";
        }
        return "\"" + object + "\"";
    }

    /**
     * Is the given string not {@code null} and not blank?
     * @param string The string to check.
     * @return true if there's something in the string.
     */
    public static boolean isNotNullOrBlank(String string) {
        return !isNullOrBlank(string);
    }

    /**
     * Is the given string {@code null} or blank?
     * @param string The string to check.
     * @return true if the string is {@code null} or blank.
     */
    public static boolean isNullOrBlank(String string) {
        return string == null || string.trim().isEmpty();
    }

    /**
     * Replace all occurrences of a substring within a string with another string.
     * @param inString {@code String} to examine
     * @param oldPattern {@code String} to replace
     * @param newPattern {@code String} to insert
     * @return a {@code String} with the replacements
     */
    public static String replace(String inString, String oldPattern, String newPattern) {
        if (noText(inString) || noText(oldPattern) || newPattern == null) {
            return inString;
        }
        int index = inString.indexOf(oldPattern);
        if (index == -1) {
            // no occurrence -> can return input as-is
            return inString;
        }

        int capacity = inString.length();
        if (newPattern.length() > oldPattern.length()) {
            capacity += 16;
        }
        StringBuilder sb = new StringBuilder(capacity);

        int pos = 0;  // our position in the old string
        int patLen = oldPattern.length();
        while (index >= 0) {
            sb.append(inString, pos, index);
            sb.append(newPattern);
            pos = index + patLen;
            index = inString.indexOf(oldPattern, pos);
        }

        // append any characters to the right of a match
        sb.append(inString, pos, inString.length());
        return sb.toString();
    }
}
