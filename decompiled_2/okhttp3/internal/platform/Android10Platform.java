/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.net.ssl.SSLSockets
 *  javax.annotation.Nullable
 *  org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
 */
package okhttp3.internal.platform;

import android.annotation.SuppressLint;
import android.net.ssl.SSLSockets;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import okhttp3.Protocol;
import okhttp3.internal.platform.AndroidPlatform;
import okhttp3.internal.platform.Platform;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

@SuppressLint(value={"NewApi"})
class Android10Platform
extends AndroidPlatform {
    Android10Platform(Class<?> sslParametersClass) {
        super(sslParametersClass, null, null, null, null, null);
    }

    @Override
    @SuppressLint(value={"NewApi"})
    @IgnoreJRERequirement
    public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols) throws IOException {
        try {
            this.enableSessionTickets(sslSocket);
            SSLParameters sslParameters = sslSocket.getSSLParameters();
            String[] protocolsArray = Platform.alpnProtocolNames(protocols).toArray(new String[0]);
            sslParameters.setApplicationProtocols(protocolsArray);
            sslSocket.setSSLParameters(sslParameters);
        }
        catch (IllegalArgumentException iae) {
            throw new IOException("Android internal error", iae);
        }
    }

    private void enableSessionTickets(SSLSocket sslSocket) {
        if (SSLSockets.isSupportedSocket((SSLSocket)sslSocket)) {
            SSLSockets.setUseSessionTickets((SSLSocket)sslSocket, (boolean)true);
        }
    }

    @Override
    @Nullable
    @IgnoreJRERequirement
    public String getSelectedProtocol(SSLSocket socket) {
        String alpnResult = socket.getApplicationProtocol();
        if (alpnResult == null || alpnResult.isEmpty()) {
            return null;
        }
        return alpnResult;
    }

    @Nullable
    public static Platform buildIfSupported() {
        if (!Platform.isAndroid()) {
            return null;
        }
        try {
            if (Android10Platform.getSdkInt() >= 29) {
                Class<?> sslParametersClass = Class.forName("com.android.org.conscrypt.SSLParametersImpl");
                return new Android10Platform(sslParametersClass);
            }
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            // empty catch block
        }
        return null;
    }
}

