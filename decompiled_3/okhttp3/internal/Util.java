/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package okhttp3.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.IDN;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.Internal;
import okhttp3.internal.http2.Header;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import okio.Options;
import okio.Source;

public final class Util {
    public static final byte[] EMPTY_BYTE_ARRAY;
    public static final String[] EMPTY_STRING_ARRAY;
    public static final Headers EMPTY_HEADERS;
    public static final ResponseBody EMPTY_RESPONSE;
    public static final RequestBody EMPTY_REQUEST;
    private static final Options UNICODE_BOMS;
    private static final Charset UTF_32BE;
    private static final Charset UTF_32LE;
    public static final TimeZone UTC;
    public static final Comparator<String> NATURAL_ORDER;
    private static final Method addSuppressedExceptionMethod;
    private static final Pattern VERIFY_AS_IP_ADDRESS;

    public static void addSuppressedIfPossible(Throwable e, Throwable suppressed) {
        if (addSuppressedExceptionMethod != null) {
            try {
                addSuppressedExceptionMethod.invoke((Object)e, suppressed);
            }
            catch (IllegalAccessException | InvocationTargetException reflectiveOperationException) {
                // empty catch block
            }
        }
    }

    private Util() {
    }

    public static void checkOffsetAndCount(long arrayLength, long offset, long count) {
        if ((offset | count) < 0L || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (RuntimeException rethrown) {
                throw rethrown;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            }
            catch (AssertionError e) {
                if (!Util.isAndroidGetsocknameError(e)) {
                    throw e;
                }
            }
            catch (RuntimeException rethrown) {
                throw rethrown;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public static void closeQuietly(ServerSocket serverSocket) {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            }
            catch (RuntimeException rethrown) {
                throw rethrown;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public static boolean discard(Source source, int timeout, TimeUnit timeUnit) {
        try {
            return Util.skipAll(source, timeout, timeUnit);
        }
        catch (IOException e) {
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean skipAll(Source source, int duration, TimeUnit timeUnit) throws IOException {
        long now = System.nanoTime();
        long originalDuration = source.timeout().hasDeadline() ? source.timeout().deadlineNanoTime() - now : Long.MAX_VALUE;
        source.timeout().deadlineNanoTime(now + Math.min(originalDuration, timeUnit.toNanos(duration)));
        try {
            Buffer skipBuffer = new Buffer();
            while (source.read(skipBuffer, 8192L) != -1L) {
                skipBuffer.clear();
            }
            boolean bl = true;
            return bl;
        }
        catch (InterruptedIOException e) {
            boolean bl = false;
            return bl;
        }
        finally {
            if (originalDuration == Long.MAX_VALUE) {
                source.timeout().clearDeadline();
            } else {
                source.timeout().deadlineNanoTime(now + originalDuration);
            }
        }
    }

    public static <T> List<T> immutableList(List<T> list) {
        return Collections.unmodifiableList(new ArrayList<T>(list));
    }

    public static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
        return map.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<K, V>(map));
    }

    @SafeVarargs
    public static <T> List<T> immutableList(T ... elements) {
        return Collections.unmodifiableList(Arrays.asList((Object[])elements.clone()));
    }

    public static ThreadFactory threadFactory(String name, boolean daemon) {
        return runnable -> {
            Thread result = new Thread(runnable, name);
            result.setDaemon(daemon);
            return result;
        };
    }

    public static String[] intersect(Comparator<? super String> comparator, String[] first, String[] second) {
        ArrayList<String> result = new ArrayList<String>();
        block0: for (String a : first) {
            for (String b : second) {
                if (comparator.compare(a, b) != 0) continue;
                result.add(a);
                continue block0;
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public static boolean nonEmptyIntersection(Comparator<String> comparator, String[] first, String[] second) {
        if (first == null || second == null || first.length == 0 || second.length == 0) {
            return false;
        }
        for (String a : first) {
            for (String b : second) {
                if (comparator.compare(a, b) != 0) continue;
                return true;
            }
        }
        return false;
    }

    public static String hostHeader(HttpUrl url, boolean includeDefaultPort) {
        String host = url.host().contains(":") ? "[" + url.host() + "]" : url.host();
        return includeDefaultPort || url.port() != HttpUrl.defaultPort(url.scheme()) ? host + ":" + url.port() : host;
    }

    public static boolean isAndroidGetsocknameError(AssertionError e) {
        return ((Throwable)((Object)e)).getCause() != null && ((Throwable)((Object)e)).getMessage() != null && ((Throwable)((Object)e)).getMessage().contains("getsockname failed");
    }

    public static int indexOf(Comparator<String> comparator, String[] array, String value) {
        int size = array.length;
        for (int i = 0; i < size; ++i) {
            if (comparator.compare(array[i], value) != 0) continue;
            return i;
        }
        return -1;
    }

    public static String[] concat(String[] array, String value) {
        String[] result = new String[array.length + 1];
        System.arraycopy(array, 0, result, 0, array.length);
        result[result.length - 1] = value;
        return result;
    }

    public static int skipLeadingAsciiWhitespace(String input, int pos, int limit) {
        block3: for (int i = pos; i < limit; ++i) {
            switch (input.charAt(i)) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    continue block3;
                }
                default: {
                    return i;
                }
            }
        }
        return limit;
    }

    public static int skipTrailingAsciiWhitespace(String input, int pos, int limit) {
        block3: for (int i = limit - 1; i >= pos; --i) {
            switch (input.charAt(i)) {
                case '\t': 
                case '\n': 
                case '\f': 
                case '\r': 
                case ' ': {
                    continue block3;
                }
                default: {
                    return i + 1;
                }
            }
        }
        return pos;
    }

    public static String trimSubstring(String string, int pos, int limit) {
        int start = Util.skipLeadingAsciiWhitespace(string, pos, limit);
        int end = Util.skipTrailingAsciiWhitespace(string, start, limit);
        return string.substring(start, end);
    }

    public static int delimiterOffset(String input, int pos, int limit, String delimiters) {
        for (int i = pos; i < limit; ++i) {
            if (delimiters.indexOf(input.charAt(i)) == -1) continue;
            return i;
        }
        return limit;
    }

    public static int delimiterOffset(String input, int pos, int limit, char delimiter) {
        for (int i = pos; i < limit; ++i) {
            if (input.charAt(i) != delimiter) continue;
            return i;
        }
        return limit;
    }

    public static String canonicalizeHost(String host) {
        if (host.contains(":")) {
            InetAddress inetAddress;
            InetAddress inetAddress2 = inetAddress = host.startsWith("[") && host.endsWith("]") ? Util.decodeIpv6(host, 1, host.length() - 1) : Util.decodeIpv6(host, 0, host.length());
            if (inetAddress == null) {
                return null;
            }
            byte[] address = inetAddress.getAddress();
            if (address.length == 16) {
                return Util.inet6AddressToAscii(address);
            }
            if (address.length == 4) {
                return inetAddress.getHostAddress();
            }
            throw new AssertionError((Object)("Invalid IPv6 address: '" + host + "'"));
        }
        try {
            String result = IDN.toASCII(host).toLowerCase(Locale.US);
            if (result.isEmpty()) {
                return null;
            }
            if (Util.containsInvalidHostnameAsciiCodes(result)) {
                return null;
            }
            return result;
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean containsInvalidHostnameAsciiCodes(String hostnameAscii) {
        for (int i = 0; i < hostnameAscii.length(); ++i) {
            char c = hostnameAscii.charAt(i);
            if (c <= '\u001f' || c >= '\u007f') {
                return true;
            }
            if (" #%/:?@[\\]".indexOf(c) == -1) continue;
            return true;
        }
        return false;
    }

    public static int indexOfControlOrNonAscii(String input) {
        int length = input.length();
        for (int i = 0; i < length; ++i) {
            char c = input.charAt(i);
            if (c > '\u001f' && c < '\u007f') continue;
            return i;
        }
        return -1;
    }

    public static boolean verifyAsIpAddress(String host) {
        return VERIFY_AS_IP_ADDRESS.matcher(host).matches();
    }

    public static String format(String format, Object ... args) {
        return String.format(Locale.US, format, args);
    }

    public static Charset bomAwareCharset(BufferedSource source, Charset charset) throws IOException {
        switch (source.select(UNICODE_BOMS)) {
            case 0: {
                return StandardCharsets.UTF_8;
            }
            case 1: {
                return StandardCharsets.UTF_16BE;
            }
            case 2: {
                return StandardCharsets.UTF_16LE;
            }
            case 3: {
                return UTF_32BE;
            }
            case 4: {
                return UTF_32LE;
            }
            case -1: {
                return charset;
            }
        }
        throw new AssertionError();
    }

    public static int checkDuration(String name, long duration, TimeUnit unit) {
        if (duration < 0L) {
            throw new IllegalArgumentException(name + " < 0");
        }
        if (unit == null) {
            throw new NullPointerException("unit == null");
        }
        long millis = unit.toMillis(duration);
        if (millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " too large.");
        }
        if (millis == 0L && duration > 0L) {
            throw new IllegalArgumentException(name + " too small.");
        }
        return (int)millis;
    }

    public static int decodeHexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - 48;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 97 + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 65 + 10;
        }
        return -1;
    }

    @Nullable
    private static InetAddress decodeIpv6(String input, int pos, int limit) {
        byte[] address = new byte[16];
        int b = 0;
        int compress = -1;
        int groupOffset = -1;
        int i = pos;
        while (i < limit) {
            char c;
            int hexDigit;
            if (b == address.length) {
                return null;
            }
            if (i + 2 <= limit && input.regionMatches(i, "::", 0, 2)) {
                if (compress != -1) {
                    return null;
                }
                compress = b += 2;
                if ((i += 2) == limit) {
                    break;
                }
            } else if (b != 0 && !input.regionMatches(i, ":", 0, 1)) {
                if (input.regionMatches(i, ".", 0, 1)) {
                    if (!Util.decodeIpv4Suffix(input, groupOffset, limit, address, b - 2)) {
                        return null;
                    }
                    b += 2;
                    break;
                }
                return null;
            }
            int value = 0;
            groupOffset = ++i;
            while (i < limit && (hexDigit = Util.decodeHexDigit(c = input.charAt(i))) != -1) {
                value = (value << 4) + hexDigit;
                ++i;
            }
            int groupLength = i - groupOffset;
            if (groupLength == 0 || groupLength > 4) {
                return null;
            }
            address[b++] = (byte)(value >>> 8 & 0xFF);
            address[b++] = (byte)(value & 0xFF);
        }
        if (b != address.length) {
            if (compress == -1) {
                return null;
            }
            System.arraycopy(address, compress, address, address.length - (b - compress), b - compress);
            Arrays.fill(address, compress, compress + (address.length - b), (byte)0);
        }
        try {
            return InetAddress.getByAddress(address);
        }
        catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

    private static boolean decodeIpv4Suffix(String input, int pos, int limit, byte[] address, int addressOffset) {
        int b = addressOffset;
        int i = pos;
        while (i < limit) {
            char c;
            if (b == address.length) {
                return false;
            }
            if (b != addressOffset && input.charAt(i) != '.') {
                return false;
            }
            int value = 0;
            int groupOffset = ++i;
            while (i < limit && (c = input.charAt(i)) >= '0' && c <= '9') {
                if (value == 0 && groupOffset != i) {
                    return false;
                }
                if ((value = value * 10 + c - 48) > 255) {
                    return false;
                }
                ++i;
            }
            int groupLength = i - groupOffset;
            if (groupLength == 0) {
                return false;
            }
            address[b++] = (byte)value;
        }
        return b == addressOffset + 4;
    }

    private static String inet6AddressToAscii(byte[] address) {
        int longestRunOffset = -1;
        int longestRunLength = 0;
        for (int i = 0; i < address.length; i += 2) {
            int currentRunOffset = i;
            while (i < 16 && address[i] == 0 && address[i + 1] == 0) {
                i += 2;
            }
            int currentRunLength = i - currentRunOffset;
            if (currentRunLength <= longestRunLength || currentRunLength < 4) continue;
            longestRunOffset = currentRunOffset;
            longestRunLength = currentRunLength;
        }
        Buffer result = new Buffer();
        int i = 0;
        while (i < address.length) {
            if (i == longestRunOffset) {
                result.writeByte(58);
                if ((i += longestRunLength) != 16) continue;
                result.writeByte(58);
                continue;
            }
            if (i > 0) {
                result.writeByte(58);
            }
            int group = (address[i] & 0xFF) << 8 | address[i + 1] & 0xFF;
            result.writeHexadecimalUnsignedLong(group);
            i += 2;
        }
        return result.readUtf8();
    }

    public static X509TrustManager platformTrustManager() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore)null);
            Object[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            return (X509TrustManager)trustManagers[0];
        }
        catch (GeneralSecurityException e) {
            throw new AssertionError("No System TLS", e);
        }
    }

    public static Headers toHeaders(List<Header> headerBlock) {
        Headers.Builder builder = new Headers.Builder();
        for (Header header : headerBlock) {
            Internal.instance.addLenient(builder, header.name.utf8(), header.value.utf8());
        }
        return builder.build();
    }

    public static List<Header> toHeaderBlock(Headers headers) {
        ArrayList<Header> result = new ArrayList<Header>();
        for (int i = 0; i < headers.size(); ++i) {
            result.add(new Header(headers.name(i), headers.value(i)));
        }
        return result;
    }

    public static String getSystemProperty(String key, @Nullable String defaultValue) {
        String value;
        try {
            value = System.getProperty(key);
        }
        catch (AccessControlException ex) {
            return defaultValue;
        }
        return value != null ? value : defaultValue;
    }

    public static boolean sameConnection(HttpUrl a, HttpUrl b) {
        return a.host().equals(b.host()) && a.port() == b.port() && a.scheme().equals(b.scheme());
    }

    static {
        Method m;
        EMPTY_BYTE_ARRAY = new byte[0];
        EMPTY_STRING_ARRAY = new String[0];
        EMPTY_HEADERS = Headers.of(new String[0]);
        EMPTY_RESPONSE = ResponseBody.create(null, EMPTY_BYTE_ARRAY);
        EMPTY_REQUEST = RequestBody.create(null, EMPTY_BYTE_ARRAY);
        UNICODE_BOMS = Options.of(ByteString.decodeHex("efbbbf"), ByteString.decodeHex("feff"), ByteString.decodeHex("fffe"), ByteString.decodeHex("0000ffff"), ByteString.decodeHex("ffff0000"));
        UTF_32BE = Charset.forName("UTF-32BE");
        UTF_32LE = Charset.forName("UTF-32LE");
        UTC = TimeZone.getTimeZone("GMT");
        NATURAL_ORDER = String::compareTo;
        try {
            m = Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class);
        }
        catch (Exception e) {
            m = null;
        }
        addSuppressedExceptionMethod = m;
        VERIFY_AS_IP_ADDRESS = Pattern.compile("([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)");
    }
}

