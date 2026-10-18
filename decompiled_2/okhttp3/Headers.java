/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
 */
package okhttp3;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nullable;
import okhttp3.internal.Util;
import okhttp3.internal.http.HttpDate;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

public final class Headers {
    private final String[] namesAndValues;

    Headers(Builder builder) {
        this.namesAndValues = builder.namesAndValues.toArray(new String[builder.namesAndValues.size()]);
    }

    private Headers(String[] namesAndValues) {
        this.namesAndValues = namesAndValues;
    }

    @Nullable
    public String get(String name) {
        return Headers.get(this.namesAndValues, name);
    }

    @Nullable
    public Date getDate(String name) {
        String value = this.get(name);
        return value != null ? HttpDate.parse(value) : null;
    }

    @Nullable
    @IgnoreJRERequirement
    public Instant getInstant(String name) {
        Date value = this.getDate(name);
        return value != null ? value.toInstant() : null;
    }

    public int size() {
        return this.namesAndValues.length / 2;
    }

    public String name(int index) {
        return this.namesAndValues[index * 2];
    }

    public String value(int index) {
        return this.namesAndValues[index * 2 + 1];
    }

    public Set<String> names() {
        TreeSet<String> result = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        int size = this.size();
        for (int i = 0; i < size; ++i) {
            result.add(this.name(i));
        }
        return Collections.unmodifiableSet(result);
    }

    public List<String> values(String name) {
        ArrayList<String> result = null;
        int size = this.size();
        for (int i = 0; i < size; ++i) {
            if (!name.equalsIgnoreCase(this.name(i))) continue;
            if (result == null) {
                result = new ArrayList<String>(2);
            }
            result.add(this.value(i));
        }
        return result != null ? Collections.unmodifiableList(result) : Collections.emptyList();
    }

    public long byteCount() {
        long result = this.namesAndValues.length * 2;
        int size = this.namesAndValues.length;
        for (int i = 0; i < size; ++i) {
            result += (long)this.namesAndValues[i].length();
        }
        return result;
    }

    public Builder newBuilder() {
        Builder result = new Builder();
        Collections.addAll(result.namesAndValues, this.namesAndValues);
        return result;
    }

    public boolean equals(@Nullable Object other) {
        return other instanceof Headers && Arrays.equals(((Headers)other).namesAndValues, this.namesAndValues);
    }

    public int hashCode() {
        return Arrays.hashCode(this.namesAndValues);
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        int size = this.size();
        for (int i = 0; i < size; ++i) {
            result.append(this.name(i)).append(": ").append(this.value(i)).append("\n");
        }
        return result.toString();
    }

    public Map<String, List<String>> toMultimap() {
        TreeMap<String, List<String>> result = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        int size = this.size();
        for (int i = 0; i < size; ++i) {
            String name = this.name(i).toLowerCase(Locale.US);
            ArrayList<String> values = (ArrayList<String>)result.get(name);
            if (values == null) {
                values = new ArrayList<String>(2);
                result.put(name, values);
            }
            values.add(this.value(i));
        }
        return result;
    }

    @Nullable
    private static String get(String[] namesAndValues, String name) {
        for (int i = namesAndValues.length - 2; i >= 0; i -= 2) {
            if (!name.equalsIgnoreCase(namesAndValues[i])) continue;
            return namesAndValues[i + 1];
        }
        return null;
    }

    public static Headers of(String ... namesAndValues) {
        int i;
        if (namesAndValues == null) {
            throw new NullPointerException("namesAndValues == null");
        }
        if (namesAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("Expected alternating header names and values");
        }
        namesAndValues = (String[])namesAndValues.clone();
        for (i = 0; i < namesAndValues.length; ++i) {
            if (namesAndValues[i] == null) {
                throw new IllegalArgumentException("Headers cannot be null");
            }
            namesAndValues[i] = namesAndValues[i].trim();
        }
        for (i = 0; i < namesAndValues.length; i += 2) {
            String name = namesAndValues[i];
            String value = namesAndValues[i + 1];
            Headers.checkName(name);
            Headers.checkValue(value, name);
        }
        return new Headers(namesAndValues);
    }

    public static Headers of(Map<String, String> headers) {
        if (headers == null) {
            throw new NullPointerException("headers == null");
        }
        String[] namesAndValues = new String[headers.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getKey() == null || header.getValue() == null) {
                throw new IllegalArgumentException("Headers cannot be null");
            }
            String name = header.getKey().trim();
            String value = header.getValue().trim();
            Headers.checkName(name);
            Headers.checkValue(value, name);
            namesAndValues[i] = name;
            namesAndValues[i + 1] = value;
            i += 2;
        }
        return new Headers(namesAndValues);
    }

    static void checkName(String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name is empty");
        }
        int length = name.length();
        for (int i = 0; i < length; ++i) {
            char c = name.charAt(i);
            if (c > ' ' && c < '\u007f') continue;
            throw new IllegalArgumentException(Util.format("Unexpected char %#04x at %d in header name: %s", c, i, name));
        }
    }

    static void checkValue(String value, String name) {
        if (value == null) {
            throw new NullPointerException("value for name " + name + " == null");
        }
        int length = value.length();
        for (int i = 0; i < length; ++i) {
            char c = value.charAt(i);
            if ((c > '\u001f' || c == '\t') && c < '\u007f') continue;
            throw new IllegalArgumentException(Util.format("Unexpected char %#04x at %d in %s value: %s", c, i, name, value));
        }
    }

    public static final class Builder {
        final List<String> namesAndValues = new ArrayList<String>(20);

        Builder addLenient(String line) {
            int index = line.indexOf(":", 1);
            if (index != -1) {
                return this.addLenient(line.substring(0, index), line.substring(index + 1));
            }
            if (line.startsWith(":")) {
                return this.addLenient("", line.substring(1));
            }
            return this.addLenient("", line);
        }

        public Builder add(String line) {
            int index = line.indexOf(":");
            if (index == -1) {
                throw new IllegalArgumentException("Unexpected header: " + line);
            }
            return this.add(line.substring(0, index).trim(), line.substring(index + 1));
        }

        public Builder add(String name, String value) {
            Headers.checkName(name);
            Headers.checkValue(value, name);
            return this.addLenient(name, value);
        }

        public Builder addUnsafeNonAscii(String name, String value) {
            Headers.checkName(name);
            return this.addLenient(name, value);
        }

        public Builder addAll(Headers headers) {
            int size = headers.size();
            for (int i = 0; i < size; ++i) {
                this.addLenient(headers.name(i), headers.value(i));
            }
            return this;
        }

        public Builder add(String name, Date value) {
            if (value == null) {
                throw new NullPointerException("value for name " + name + " == null");
            }
            this.add(name, HttpDate.format(value));
            return this;
        }

        @IgnoreJRERequirement
        public Builder add(String name, Instant value) {
            if (value == null) {
                throw new NullPointerException("value for name " + name + " == null");
            }
            return this.add(name, new Date(value.toEpochMilli()));
        }

        public Builder set(String name, Date value) {
            if (value == null) {
                throw new NullPointerException("value for name " + name + " == null");
            }
            this.set(name, HttpDate.format(value));
            return this;
        }

        @IgnoreJRERequirement
        public Builder set(String name, Instant value) {
            if (value == null) {
                throw new NullPointerException("value for name " + name + " == null");
            }
            return this.set(name, new Date(value.toEpochMilli()));
        }

        Builder addLenient(String name, String value) {
            this.namesAndValues.add(name);
            this.namesAndValues.add(value.trim());
            return this;
        }

        public Builder removeAll(String name) {
            for (int i = 0; i < this.namesAndValues.size(); i += 2) {
                if (!name.equalsIgnoreCase(this.namesAndValues.get(i))) continue;
                this.namesAndValues.remove(i);
                this.namesAndValues.remove(i);
                i -= 2;
            }
            return this;
        }

        public Builder set(String name, String value) {
            Headers.checkName(name);
            Headers.checkValue(value, name);
            this.removeAll(name);
            this.addLenient(name, value);
            return this;
        }

        @Nullable
        public String get(String name) {
            for (int i = this.namesAndValues.size() - 2; i >= 0; i -= 2) {
                if (!name.equalsIgnoreCase(this.namesAndValues.get(i))) continue;
                return this.namesAndValues.get(i + 1);
            }
            return null;
        }

        public Headers build() {
            return new Headers(this);
        }
    }
}

