/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package okio;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import okio.Buffer;
import okio.ByteString;
import okio.ForwardingSink;
import okio.Segment;
import okio.Sink;
import okio.Util;

public final class HashingSink
extends ForwardingSink {
    @Nullable
    private final MessageDigest messageDigest;
    @Nullable
    private final Mac mac;

    public static HashingSink md5(Sink sink) {
        return new HashingSink(sink, "MD5");
    }

    public static HashingSink sha1(Sink sink) {
        return new HashingSink(sink, "SHA-1");
    }

    public static HashingSink sha256(Sink sink) {
        return new HashingSink(sink, "SHA-256");
    }

    public static HashingSink sha512(Sink sink) {
        return new HashingSink(sink, "SHA-512");
    }

    public static HashingSink hmacSha1(Sink sink, ByteString key) {
        return new HashingSink(sink, key, "HmacSHA1");
    }

    public static HashingSink hmacSha256(Sink sink, ByteString key) {
        return new HashingSink(sink, key, "HmacSHA256");
    }

    public static HashingSink hmacSha512(Sink sink, ByteString key) {
        return new HashingSink(sink, key, "HmacSHA512");
    }

    private HashingSink(Sink sink, String algorithm) {
        super(sink);
        try {
            this.messageDigest = MessageDigest.getInstance(algorithm);
            this.mac = null;
        }
        catch (NoSuchAlgorithmException e) {
            throw new AssertionError();
        }
    }

    private HashingSink(Sink sink, ByteString key, String algorithm) {
        super(sink);
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
    public void write(Buffer source, long byteCount) throws IOException {
        int toHash;
        Util.checkOffsetAndCount(source.size, 0L, byteCount);
        Segment s = source.head;
        for (long hashedCount = 0L; hashedCount < byteCount; hashedCount += (long)toHash) {
            toHash = (int)Math.min(byteCount - hashedCount, (long)(s.limit - s.pos));
            if (this.messageDigest != null) {
                this.messageDigest.update(s.data, s.pos, toHash);
            } else {
                this.mac.update(s.data, s.pos, toHash);
            }
            s = s.next;
        }
        super.write(source, byteCount);
    }

    public final ByteString hash() {
        byte[] result = this.messageDigest != null ? this.messageDigest.digest() : this.mac.doFinal();
        return ByteString.of(result);
    }
}

