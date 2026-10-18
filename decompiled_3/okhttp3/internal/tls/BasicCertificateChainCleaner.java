/*
 * Decompiled with CFR 0.152.
 */
package okhttp3.internal.tls;

import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.net.ssl.SSLPeerUnverifiedException;
import okhttp3.internal.tls.CertificateChainCleaner;
import okhttp3.internal.tls.TrustRootIndex;

public final class BasicCertificateChainCleaner
extends CertificateChainCleaner {
    private static final int MAX_SIGNERS = 9;
    private final TrustRootIndex trustRootIndex;

    public BasicCertificateChainCleaner(TrustRootIndex trustRootIndex) {
        this.trustRootIndex = trustRootIndex;
    }

    @Override
    public List<Certificate> clean(List<Certificate> chain, String hostname) throws SSLPeerUnverifiedException {
        ArrayDeque<Certificate> queue = new ArrayDeque<Certificate>(chain);
        ArrayList<Certificate> result = new ArrayList<Certificate>();
        result.add((Certificate)queue.removeFirst());
        boolean foundTrustedCertificate = false;
        block0: for (int c = 0; c < 9; ++c) {
            X509Certificate toVerify = (X509Certificate)result.get(result.size() - 1);
            X509Certificate trustedCert = this.trustRootIndex.findByIssuerAndSignature(toVerify);
            if (trustedCert != null) {
                if (result.size() > 1 || !toVerify.equals(trustedCert)) {
                    result.add(trustedCert);
                }
                if (this.verifySignature(trustedCert, trustedCert)) {
                    return result;
                }
                foundTrustedCertificate = true;
                continue;
            }
            Iterator i = queue.iterator();
            while (i.hasNext()) {
                X509Certificate signingCert = (X509Certificate)i.next();
                if (!this.verifySignature(toVerify, signingCert)) continue;
                i.remove();
                result.add(signingCert);
                continue block0;
            }
            if (foundTrustedCertificate) {
                return result;
            }
            throw new SSLPeerUnverifiedException("Failed to find a trusted cert that signed " + toVerify);
        }
        throw new SSLPeerUnverifiedException("Certificate chain too long: " + result);
    }

    private boolean verifySignature(X509Certificate toVerify, X509Certificate signingCert) {
        if (!toVerify.getIssuerDN().equals(signingCert.getSubjectDN())) {
            return false;
        }
        try {
            toVerify.verify(signingCert.getPublicKey());
            return true;
        }
        catch (GeneralSecurityException verifyFailed) {
            return false;
        }
    }

    public int hashCode() {
        return this.trustRootIndex.hashCode();
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        return other instanceof BasicCertificateChainCleaner && ((BasicCertificateChainCleaner)other).trustRootIndex.equals(this.trustRootIndex);
    }
}

