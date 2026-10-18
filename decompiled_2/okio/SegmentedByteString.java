/*
 * Decompiled with CFR 0.152.
 */
package okio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import okio.Buffer;
import okio.ByteString;
import okio.Segment;
import okio.Util;

final class SegmentedByteString
extends ByteString {
    final transient byte[][] segments;
    final transient int[] directory;

    SegmentedByteString(Buffer buffer, int byteCount) {
        super(null);
        Util.checkOffsetAndCount(buffer.size, 0L, byteCount);
        int offset = 0;
        int segmentCount = 0;
        Segment s = buffer.head;
        while (offset < byteCount) {
            if (s.limit == s.pos) {
                throw new AssertionError((Object)"s.limit == s.pos");
            }
            offset += s.limit - s.pos;
            ++segmentCount;
            s = s.next;
        }
        this.segments = new byte[segmentCount][];
        this.directory = new int[segmentCount * 2];
        offset = 0;
        segmentCount = 0;
        s = buffer.head;
        while (offset < byteCount) {
            this.segments[segmentCount] = s.data;
            if ((offset += s.limit - s.pos) > byteCount) {
                offset = byteCount;
            }
            this.directory[segmentCount] = offset;
            this.directory[segmentCount + this.segments.length] = s.pos;
            s.shared = true;
            ++segmentCount;
            s = s.next;
        }
    }

    @Override
    public String utf8() {
        return this.toByteString().utf8();
    }

    @Override
    public String string(Charset charset) {
        return this.toByteString().string(charset);
    }

    @Override
    public String base64() {
        return this.toByteString().base64();
    }

    @Override
    public String hex() {
        return this.toByteString().hex();
    }

    @Override
    public ByteString toAsciiLowercase() {
        return this.toByteString().toAsciiLowercase();
    }

    @Override
    public ByteString toAsciiUppercase() {
        return this.toByteString().toAsciiUppercase();
    }

    @Override
    public ByteString md5() {
        return this.toByteString().md5();
    }

    @Override
    public ByteString sha1() {
        return this.toByteString().sha1();
    }

    @Override
    public ByteString sha256() {
        return this.toByteString().sha256();
    }

    @Override
    public ByteString hmacSha1(ByteString key) {
        return this.toByteString().hmacSha1(key);
    }

    @Override
    public ByteString hmacSha256(ByteString key) {
        return this.toByteString().hmacSha256(key);
    }

    @Override
    public String base64Url() {
        return this.toByteString().base64Url();
    }

    @Override
    public ByteString substring(int beginIndex) {
        return this.toByteString().substring(beginIndex);
    }

    @Override
    public ByteString substring(int beginIndex, int endIndex) {
        return this.toByteString().substring(beginIndex, endIndex);
    }

    @Override
    public byte getByte(int pos) {
        Util.checkOffsetAndCount(this.directory[this.segments.length - 1], pos, 1L);
        int segment = this.segment(pos);
        int segmentOffset = segment == 0 ? 0 : this.directory[segment - 1];
        int segmentPos = this.directory[segment + this.segments.length];
        return this.segments[segment][pos - segmentOffset + segmentPos];
    }

    private int segment(int pos) {
        int i = Arrays.binarySearch(this.directory, 0, this.segments.length, pos + 1);
        return i >= 0 ? i : ~i;
    }

    @Override
    public int size() {
        return this.directory[this.segments.length - 1];
    }

    @Override
    public byte[] toByteArray() {
        byte[] result = new byte[this.directory[this.segments.length - 1]];
        int segmentOffset = 0;
        int segmentCount = this.segments.length;
        for (int s = 0; s < segmentCount; ++s) {
            int segmentPos = this.directory[segmentCount + s];
            int nextSegmentOffset = this.directory[s];
            System.arraycopy(this.segments[s], segmentPos, result, segmentOffset, nextSegmentOffset - segmentOffset);
            segmentOffset = nextSegmentOffset;
        }
        return result;
    }

    @Override
    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(this.toByteArray()).asReadOnlyBuffer();
    }

    @Override
    public void write(OutputStream out) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("out == null");
        }
        int segmentOffset = 0;
        int segmentCount = this.segments.length;
        for (int s = 0; s < segmentCount; ++s) {
            int segmentPos = this.directory[segmentCount + s];
            int nextSegmentOffset = this.directory[s];
            out.write(this.segments[s], segmentPos, nextSegmentOffset - segmentOffset);
            segmentOffset = nextSegmentOffset;
        }
    }

    @Override
    void write(Buffer buffer) {
        int segmentOffset = 0;
        int segmentCount = this.segments.length;
        for (int s = 0; s < segmentCount; ++s) {
            int segmentPos = this.directory[segmentCount + s];
            int nextSegmentOffset = this.directory[s];
            Segment segment = new Segment(this.segments[s], segmentPos, segmentPos + nextSegmentOffset - segmentOffset, true, false);
            if (buffer.head == null) {
                segment.next = segment.prev = segment;
                buffer.head = segment.prev;
            } else {
                buffer.head.prev.push(segment);
            }
            segmentOffset = nextSegmentOffset;
        }
        buffer.size += (long)segmentOffset;
    }

    @Override
    public boolean rangeEquals(int offset, ByteString other, int otherOffset, int byteCount) {
        if (offset < 0 || offset > this.size() - byteCount) {
            return false;
        }
        int s = this.segment(offset);
        while (byteCount > 0) {
            int segmentSize;
            int stepSize;
            int segmentPos;
            int segmentOffset = s == 0 ? 0 : this.directory[s - 1];
            int arrayOffset = offset - segmentOffset + (segmentPos = this.directory[this.segments.length + s]);
            if (!other.rangeEquals(otherOffset, this.segments[s], arrayOffset, stepSize = Math.min(byteCount, segmentOffset + (segmentSize = this.directory[s] - segmentOffset) - offset))) {
                return false;
            }
            offset += stepSize;
            otherOffset += stepSize;
            byteCount -= stepSize;
            ++s;
        }
        return true;
    }

    @Override
    public boolean rangeEquals(int offset, byte[] other, int otherOffset, int byteCount) {
        if (offset < 0 || offset > this.size() - byteCount || otherOffset < 0 || otherOffset > other.length - byteCount) {
            return false;
        }
        int s = this.segment(offset);
        while (byteCount > 0) {
            int segmentSize;
            int stepSize;
            int segmentPos;
            int segmentOffset = s == 0 ? 0 : this.directory[s - 1];
            int arrayOffset = offset - segmentOffset + (segmentPos = this.directory[this.segments.length + s]);
            if (!Util.arrayRangeEquals(this.segments[s], arrayOffset, other, otherOffset, stepSize = Math.min(byteCount, segmentOffset + (segmentSize = this.directory[s] - segmentOffset) - offset))) {
                return false;
            }
            offset += stepSize;
            otherOffset += stepSize;
            byteCount -= stepSize;
            ++s;
        }
        return true;
    }

    @Override
    public int indexOf(byte[] other, int fromIndex) {
        return this.toByteString().indexOf(other, fromIndex);
    }

    @Override
    public int lastIndexOf(byte[] other, int fromIndex) {
        return this.toByteString().lastIndexOf(other, fromIndex);
    }

    private ByteString toByteString() {
        return new ByteString(this.toByteArray());
    }

    @Override
    byte[] internalArray() {
        return this.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        return o instanceof ByteString && ((ByteString)o).size() == this.size() && this.rangeEquals(0, (ByteString)o, 0, this.size());
    }

    @Override
    public int hashCode() {
        int result = this.hashCode;
        if (result != 0) {
            return result;
        }
        result = 1;
        int segmentOffset = 0;
        int segmentCount = this.segments.length;
        for (int s = 0; s < segmentCount; ++s) {
            byte[] segment = this.segments[s];
            int segmentPos = this.directory[segmentCount + s];
            int nextSegmentOffset = this.directory[s];
            int segmentSize = nextSegmentOffset - segmentOffset;
            int limit = segmentPos + segmentSize;
            for (int i = segmentPos; i < limit; ++i) {
                result = 31 * result + segment[i];
            }
            segmentOffset = nextSegmentOffset;
        }
        this.hashCode = result;
        return this.hashCode;
    }

    @Override
    public String toString() {
        return this.toByteString().toString();
    }

    private Object writeReplace() {
        return this.toByteString();
    }
}

