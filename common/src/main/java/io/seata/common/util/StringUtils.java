/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata.common.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.seata.common.Constants;
import io.seata.common.exception.ShouldNeverHappenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type String utils.
 *
 */
public class StringUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringUtils.class);
    private static final Pattern CAMEL_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LINE_PATTERN = Pattern.compile("-(\\w)");

    private StringUtils() {
    }

    /**
     * empty string
     */
    public static final String EMPTY = "";

    /**
     * Space string
     */
    public static final String SPACE = " ";

    /**
     * Is empty boolean.
     *
     * @param str the str
     * @return the boolean
     */
    public static boolean isNullOrEmpty(String str) {
        return (str == null) || (str.isEmpty());
    }

    /**
     * Is blank string ?
     *
     * @param str the str
     * @return boolean boolean
     */
    public static boolean isBlank(String str) {
        int length;

        if ((str == null) || ((length = str.length()) == 0)) {
            return true;
        }
        for (int i = 0; i < length; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Is Not blank string ?
     *
     * @param str the str
     * @return boolean boolean
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Equals boolean.
     *
     * @param a the a
     * @param b the b
     * @return boolean
     */
    public static boolean equals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    /**
     * Equals ignore case boolean.
     *
     * @param a the a
     * @param b the b
     * @return the boolean
     */
    public static boolean equalsIgnoreCase(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equalsIgnoreCase(b);
    }

    /**
     * Input stream 2 string string.
     *
     * @param is the is
     * @return the string
     */
    public static String inputStream2String(InputStream is) {
        if (is == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int i;
            while ((i = is.read()) != -1) {
                baos.write(i);
            }
            return baos.toString(Constants.DEFAULT_CHARSET_NAME);
        } catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Input stream to byte array
     *
     * @param is the is
     * @return the byte array
     */
    public static byte[] inputStream2Bytes(InputStream is) {
        if (is == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int i;
            while ((i = is.read()) != -1) {
                baos.write(i);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Object.toString()
     *
     * @param obj the obj
     * @return string string
     */
    @SuppressWarnings("deprecation")
    public static String toString(final Object obj) {
        if (obj == null) {
            return "null";
        }

        //region Convert simple types to String directly

        if (obj instanceof CharSequence) {
            return "\"" + obj + "\"";
        }
        if (obj instanceof Character) {
            return "'" + obj + "'";
        }
        if (obj instanceof Date) {
            Date date = (Date)obj;
            long time = date.getTime();
            String dateFormat;
            if (date.getHours() == 0 && date.getMinutes() == 0 && date.getSeconds() == 0 && time % 1000 == 0) {
                dateFormat = "yyyy-MM-dd";
            } else if (time % (60 * 1000) == 0) {
                dateFormat = "yyyy-MM-dd HH:mm";
            } else if (time % 1000 == 0) {
                dateFormat = "yyyy-MM-dd HH:mm:ss";
            } else {
                dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
            }
            return new SimpleDateFormat(dateFormat).format(obj);
        }
        if (obj instanceof Enum) {
            return obj.getClass().getSimpleName() + "." + ((Enum)obj).name();
        }
        if (obj instanceof Class) {
            return ReflectionUtil.classToString((Class<?>)obj);
        }
        if (obj instanceof Field) {
            return ReflectionUtil.fieldToString((Field)obj);
        }
        if (obj instanceof Method) {
            return ReflectionUtil.methodToString((Method)obj);
        }
        if (obj instanceof Annotation) {
            return ReflectionUtil.annotationToString((Annotation)obj);
        }

        //endregion

        //region Convert the Collection and Map

        if (obj instanceof Collection) {
            return CollectionUtils.toString((Collection<?>)obj);
        }
        if (obj.getClass().isArray()) {
            return ArrayUtils.toString(obj);
        }
        if (obj instanceof Map) {
            return CollectionUtils.toString((Map<?, ?>)obj);
        }

        //endregion

        //the jdk classes
        if (obj.getClass().getClassLoader() == null) {
            return obj.toString();
        }

        return CycleDependencyHandler.wrap(obj, o -> {
            StringBuilder sb = new StringBuilder(32);

            // handle the anonymous class
            String classSimpleName;
            if (obj.getClass().isAnonymousClass()) {
                if (!obj.getClass().getSuperclass().equals(Object.class)) {
                    classSimpleName = obj.getClass().getSuperclass().getSimpleName();
                } else {
                    classSimpleName = obj.getClass().getInterfaces()[0].getSimpleName();
                }
                // Connect a '$', different from ordinary class
                classSimpleName += "$";
            } else {
                classSimpleName = obj.getClass().getSimpleName();
            }

            sb.append(classSimpleName).append("(");
            final int initialLength = sb.length();

            // Gets all fields, excluding static or synthetic fields
            Field[] fields = ReflectionUtil.getAllFields(obj.getClass());
            for (Field field : fields) {
                field.setAccessible(true);

                if (sb.length() > initialLength) {
                    sb.append(", ");
                }
                sb.append(field.getName());
                sb.append("=");
                try {
                    Object f = field.get(obj);
                    if (f == obj) {
                        sb.append("(this ").append(f.getClass().getSimpleName()).append(")");
                    } else {
                        sb.append(toString(f));
                    }
                } catch (Exception ignore) {
                }
            }

            sb.append(")");
            return sb.toString();
        });
    }

    /**
     * Trim string to null if empty("").
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String
     */
    public static String trimToNull(final String str) {
        final String ts = trim(str);
        return isEmpty(ts) ? null : ts;
    }

    /**
     * Trim string, or null if string is null.
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed string, {@code null} if null String input
     */
    public static String trim(final String str) {
        return str == null ? null : str.trim();
    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Checks if a CharSequence is not empty ("") and not null.
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * hump to Line or line to hump, only spring environment use
     * 
     * @param str str
     * @return string string
     */
    public static String hump2Line(String str) {
        Matcher matcher = CAMEL_PATTERN.matcher(str);
        StringBuffer sb = new StringBuffer();
        if (matcher.find()) {
            matcher.appendReplacement(sb, "-" + matcher.group(0).toLowerCase());
            while (matcher.find()) {
                matcher.appendReplacement(sb, "-" + matcher.group(0).toLowerCase());
            }
        } else {
            matcher = LINE_PATTERN.matcher(str);
            while (matcher.find()) {
                matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * check string data size
     *
     * @param data the str
     * @param dataName the data name
     * @param errorSize throw exception if size > errorSize
     * @return boolean
     */
    public static boolean checkDataSize(String data, String dataName, int errorSize, boolean throwIfErr) {
        if (isBlank(data)) {
            return true;
        }
        int length = data.getBytes(StandardCharsets.UTF_8).length;
        if (length > errorSize) {
            LOGGER.warn("{} data is large(errorSize), size={}", dataName, length);
            if (!throwIfErr) {
                return false;
            }
            throw new IllegalArgumentException(dataName + " data is too large, size=" + length);
        }
        return true;
    }

    public static boolean hasLowerCase(String str) {
        if (null == str) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (Character.isLowerCase(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasUpperCase(String str) {
        if (null == str) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (Character.isUpperCase(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

}
