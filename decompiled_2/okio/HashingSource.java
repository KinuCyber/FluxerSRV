/*
 * Decompiled with CFR 0.152.
 */
package okio;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import okio.Buffer;
import okio.ByteString;
import okio.ForwardingSource;
import okio.Segment;
import okio.Source;

public final class HashingSource
extends ForwardingSource {
    private final MessageDigest messageDigest;
    private final Mac mac;

    public static HashingSource md5(Source source) {
        return new HashingSource(source, "MD5");
    }

    public static HashingSource sha1(Source source) {
        return new HashingSource(source, "SHA-1");
    }

    public static HashingSource sha256(Source source) {
        return new HashingSource(source, "SHA-256");
    }

    public static HashingSource hmacSha1(Source source, ByteString key) {
        return new HashingSource(source, key, "HmacSHA1");
    }

    public static HashingSource hmacSha256(Source source, ByteString key) {
        return new HashingSource(source, key, "HmacSHA256");
    }

    private HashingSource(Source source, String algorithm) {
        super(source);
        try {
            this.messageDigest = MessageDigest.getInstance(algorithm);
            this.mac = null;
        }
        catch (NoSuchAlgorithmException e) {
            throw new AssertionError();
        }
    }

    private HashingSource(Source source, ByteString key, String algorithm) {
        super(source);
        try {
            this.mac = Mac.getInstance(algorithm);
            this.mac.init(new SecretKeySpec(key.toByteArray(), algorithm));
            this.messageDigest = null;
        }
        catch (NoSuchAlgorithmException e) {
            throw new AssertionError();
        }
        catch (InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        long result = super.read(sink, byteCount);
        if (result != -1L) {
            long offset;
            long start = sink.size - result;
            Segment s = sink.head;
            for (offset = sink.size; offset > start; offset -= (long)(s.limit - s.pos)) {
                s = s.prev;
            }
            while (offset < sink.size) {
                int pos = (int)((long)s.pos + start - offset);
                if (this.messageDigest != null) {
                    this.messageDigest.update(s.data, pos, s.limit - pos);
                } else {
                    this.mac.update(s.data, pos, s.limit - pos);
                }
                start = offset += (long)(s.limit - s.pos);
                s = s.next;
            }
        }
        return result;
    }

    public final ByteString hash() {
        byte[] result = this.messageDigest != null ? this.messageDigest.digest() : this.mac.doFinal();
        return ByteString.of(result);
    }
}

