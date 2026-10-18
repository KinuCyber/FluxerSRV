/*
 * Decompiled with CFR 0.152.
 */
package okhttp3.internal.connection;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ProtocolException;
import java.net.UnknownServiceException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import okhttp3.ConnectionSpec;
import okhttp3.internal.Internal;

final class ConnectionSpecSelector {
    private final List<ConnectionSpec> connectionSpecs;
    private int nextModeIndex = 0;
    private boolean isFallbackPossible;
    private boolean isFallback;

    ConnectionSpecSelector(List<ConnectionSpec> connectionSpecs) {
        this.connectionSpecs = connectionSpecs;
    }

    ConnectionSpec configureSecureSocket(SSLSocket sslSocket) throws IOException {
        ConnectionSpec tlsConfiguration = null;
        int size = this.connectionSpecs.size();
        for (int i = this.nextModeIndex; i < size; ++i) {
            ConnectionSpec connectionSpec = this.connectionSpecs.get(i);
            if (!connectionSpec.isCompatible(sslSocket)) continue;
            tlsConfiguration = connectionSpec;
            this.nextModeIndex = i + 1;
            break;
        }
        if (tlsConfiguration == null) {
            throw new UnknownServiceException("Unable to find acceptable protocols. isFallback=" + this.isFallback + ", modes=" + this.connectionSpecs + ", supported protocols=" + Arrays.toString(sslSocket.getEnabledProtocols()));
        }
        this.isFallbackPossible = this.isFallbackPossible(sslSocket);
        Internal.instance.apply(tlsConfiguration, sslSocket, this.isFallback);
        return tlsConfiguration;
    }

    boolean connectionFailed(IOException e) {
        this.isFallback = true;
        if (!this.isFallbackPossible) {
            return false;
        }
        if (e instanceof ProtocolException) {
            return false;
        }
        if (e instanceof InterruptedIOException) {
            return false;
        }
        if (e instanceof SSLHandshakeException && e.getCause() instanceof CertificateException) {
            return false;
        }
        if (e instanceof SSLPeerUnverifiedException) {
            return false;
        }
        return e instanceof SSLException;
    }

    private boolean isFallbackPossible(SSLSocket socket) {
        for (int i = this.nextModeIndex; i < this.connectionSpecs.size(); ++i) {
            if (!this.connectionSpecs.get(i).isCompatible(socket)) continue;
            return true;
        }
        return false;
    }
}

