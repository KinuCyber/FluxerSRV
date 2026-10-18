/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  org.conscrypt.Conscrypt
 */
package okhttp3.internal.platform;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.List;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Protocol;
import okhttp3.internal.platform.Platform;
import org.conscrypt.Conscrypt;

public class ConscryptPlatform
extends Platform {
    private ConscryptPlatform() {
    }

    private Provider getProvider() {
        return Conscrypt.newProviderBuilder().provideTrustManager().build();
    }

    @Override
    @Nullable
    public X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
        if (!Conscrypt.isConscrypt((SSLSocketFactory)sslSocketFactory)) {
            return super.trustManager(sslSocketFactory);
        }
        try {
            Object sp = ConscryptPlatform.readFieldOrNull(sslSocketFactory, Object.class, "sslParameters");
            if (sp != null) {
                return ConscryptPlatform.readFieldOrNull(sp, X509TrustManager.class, "x509TrustManager");
            }
            return null;
        }
        catch (Exception e) {
            throw new UnsupportedOperationException("clientBuilder.sslSocketFactory(SSLSocketFactory) not supported on Conscrypt", e);
        }
    }

    @Override
    public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols) throws IOException {
        if (Conscrypt.isConscrypt((SSLSocket)sslSocket)) {
            if (hostname != null) {
                Conscrypt.setUseSessionTickets((SSLSocket)sslSocket, (boolean)true);
                Conscrypt.setHostname((SSLSocket)sslSocket, (String)hostname);
            }
            List<String> names = Platform.alpnProtocolNames(protocols);
            Conscrypt.setApplicationProtocols((SSLSocket)sslSocket, (String[])names.toArray(new String[0]));
        } else {
            super.configureTlsExtensions(sslSocket, hostname, protocols);
        }
    }

    @Override
    @Nullable
    public String getSelectedProtocol(SSLSocket sslSocket) {
        if (Conscrypt.isConscrypt((SSLSocket)sslSocket)) {
            return Conscrypt.getApplicationProtocol((SSLSocket)sslSocket);
        }
        return super.getSelectedProtocol(sslSocket);
    }

    @Override
    public SSLContext getSSLContext() {
        try {
            return SSLContext.getInstance("TLSv1.3", this.getProvider());
        }
        catch (NoSuchAlgorithmException e) {
            try {
                return SSLContext.getInstance("TLS", this.getProvider());
            }
            catch (NoSuchAlgorithmException e2) {
                throw new IllegalStateException("No TLS provider", e);
            }
        }
    }

    public static ConscryptPlatform buildIfSupported() {
        try {
            Class.forName("org.conscrypt.Conscrypt");
            if (!Conscrypt.isAvailable()) {
                return null;
            }
            return new ConscryptPlatform();
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public void configureSslSocketFactory(SSLSocketFactory socketFactory) {
        if (Conscrypt.isConscrypt((SSLSocketFactory)socketFactory)) {
            Conscrypt.setUseEngineSocket((SSLSocketFactory)socketFactory, (boolean)true);
        }
    }
}

