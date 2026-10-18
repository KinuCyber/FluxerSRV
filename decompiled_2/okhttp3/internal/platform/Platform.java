/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package okhttp3.internal.platform;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.internal.Util;
import okhttp3.internal.platform.Android10Platform;
import okhttp3.internal.platform.AndroidPlatform;
import okhttp3.internal.platform.ConscryptPlatform;
import okhttp3.internal.platform.Jdk8WithJettyBootPlatform;
import okhttp3.internal.platform.Jdk9Platform;
import okhttp3.internal.tls.BasicCertificateChainCleaner;
import okhttp3.internal.tls.BasicTrustRootIndex;
import okhttp3.internal.tls.CertificateChainCleaner;
import okhttp3.internal.tls.TrustRootIndex;
import okio.Buffer;

public class Platform {
    private static final Platform PLATFORM = Platform.findPlatform();
    public static final int INFO = 4;
    public static final int WARN = 5;
    private static final Logger logger = Logger.getLogger(OkHttpClient.class.getName());

    public static Platform get() {
        return PLATFORM;
    }

    public String getPrefix() {
        return "OkHttp";
    }

    @Nullable
    protected X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
        try {
            Class<?> sslContextClass = Class.forName("sun.security.ssl.SSLContextImpl");
            Object context = Platform.readFieldOrNull(sslSocketFactory, sslContextClass, "context");
            if (context == null) {
                return null;
            }
            return Platform.readFieldOrNull(context, X509TrustManager.class, "trustManager");
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }

    public void configureTlsExtensions(SSLSocket sslSocket, @Nullable String hostname, List<Protocol> protocols) throws IOException {
    }

    public void afterHandshake(SSLSocket sslSocket) {
    }

    @Nullable
    public String getSelectedProtocol(SSLSocket socket) {
        return null;
    }

    public void connectSocket(Socket socket, InetSocketAddress address, int connectTimeout) throws IOException {
        socket.connect(address, connectTimeout);
    }

    public void log(int level, String message, @Nullable Throwable t) {
        Level logLevel = level == 5 ? Level.WARNING : Level.INFO;
        logger.log(logLevel, message, t);
    }

    public boolean isCleartextTrafficPermitted(String hostname) {
        return true;
    }

    @Nullable
    public Object getStackTraceForCloseable(String closer) {
        if (logger.isLoggable(Level.FINE)) {
            return new Throwable(closer);
        }
        return null;
    }

    public void logCloseableLeak(String message, Object stackTrace) {
        if (stackTrace == null) {
            message = message + " To see where this was allocated, set the OkHttpClient logger level to FINE: Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);";
        }
        this.log(5, message, (Throwable)stackTrace);
    }

    public static List<String> alpnProtocolNames(List<Protocol> protocols) {
        ArrayList<String> names = new ArrayList<String>(protocols.size());
        int size = protocols.size();
        for (int i = 0; i < size; ++i) {
            Protocol protocol = protocols.get(i);
            if (protocol == Protocol.HTTP_1_0) continue;
            names.add(protocol.toString());
        }
        return names;
    }

    public CertificateChainCleaner buildCertificateChainCleaner(X509TrustManager trustManager) {
        return new BasicCertificateChainCleaner(this.buildTrustRootIndex(trustManager));
    }

    public CertificateChainCleaner buildCertificateChainCleaner(SSLSocketFactory sslSocketFactory) {
        X509TrustManager trustManager = this.trustManager(sslSocketFactory);
        if (trustManager == null) {
            throw new IllegalStateException("Unable to extract the trust manager on " + Platform.get() + ", sslSocketFactory is " + sslSocketFactory.getClass());
        }
        return this.buildCertificateChainCleaner(trustManager);
    }

    public static boolean isConscryptPreferred() {
        if ("conscrypt".equals(Util.getSystemProperty("okhttp.platform", null))) {
            return true;
        }
        String preferredProvider = Security.getProviders()[0].getName();
        return "Conscrypt".equals(preferredProvider);
    }

    private static Platform findPlatform() {
        if (Platform.isAndroid()) {
            return Platform.findAndroidPlatform();
        }
        return Platform.findJvmPlatform();
    }

    public static boolean isAndroid() {
        return "Dalvik".equals(System.getProperty("java.vm.name"));
    }

    private static Platform findJvmPlatform() {
        ConscryptPlatform conscrypt;
        if (Platform.isConscryptPreferred() && (conscrypt = ConscryptPlatform.buildIfSupported()) != null) {
            return conscrypt;
        }
        Jdk9Platform jdk9 = Jdk9Platform.buildIfSupported();
        if (jdk9 != null) {
            return jdk9;
        }
        Platform jdkWithJettyBoot = Jdk8WithJettyBootPlatform.buildIfSupported();
        if (jdkWithJettyBoot != null) {
            return jdkWithJettyBoot;
        }
        return new Platform();
    }

    private static Platform findAndroidPlatform() {
        Platform android10 = Android10Platform.buildIfSupported();
        if (android10 != null) {
            return android10;
        }
        Platform android = AndroidPlatform.buildIfSupported();
        if (android == null) {
            throw new NullPointerException("No platform found on Android");
        }
        return android;
    }

    static byte[] concatLengthPrefixed(List<Protocol> protocols) {
        Buffer result = new Buffer();
        int size = protocols.size();
        for (int i = 0; i < size; ++i) {
            Protocol protocol = protocols.get(i);
            if (protocol == Protocol.HTTP_1_0) continue;
            result.writeByte(protocol.toString().length());
            result.writeUtf8(protocol.toString());
        }
        return result.readByteArray();
    }

    @Nullable
    static <T> T readFieldOrNull(Object instance, Class<T> fieldType, String fieldName) {
        Object delegate;
        for (Class<?> c = instance.getClass(); c != Object.class; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(instance);
                if (!fieldType.isInstance(value)) {
                    return null;
                }
                return fieldType.cast(value);
            }
            catch (NoSuchFieldException field) {
                continue;
            }
            catch (IllegalAccessException e) {
                throw new AssertionError();
            }
        }
        if (!fieldName.equals("delegate") && (delegate = Platform.readFieldOrNull(instance, Object.class, "delegate")) != null) {
            return Platform.readFieldOrNull(delegate, fieldType, fieldName);
        }
        return null;
    }

    public SSLContext getSSLContext() {
        try {
            return SSLContext.getInstance("TLS");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No TLS provider", e);
        }
    }

    public TrustRootIndex buildTrustRootIndex(X509TrustManager trustManager) {
        return new BasicTrustRootIndex(trustManager.getAcceptedIssuers());
    }

    public void configureSslSocketFactory(SSLSocketFactory socketFactory) {
    }

    public String toString() {
        return this.getClass().getSimpleName();
    }
}

