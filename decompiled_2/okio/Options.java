/*
 * Decompiled with CFR 0.152.
 */
package okio;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;
import okio.Buffer;
import okio.ByteString;

public final class Options
extends AbstractList<ByteString>
implements RandomAccess {
    final ByteString[] byteStrings;
    final int[] trie;

    private Options(ByteString[] byteStrings, int[] trie) {
        this.byteStrings = byteStrings;
        this.trie = trie;
    }

    public static Options of(ByteString ... byteStrings) {
        int i;
        if (byteStrings.length == 0) {
            return new Options(new ByteString[0], new int[]{0, -1});
        }
        ArrayList<ByteString> list = new ArrayList<ByteString>(Arrays.asList(byteStrings));
        Collections.sort(list);
        ArrayList<Integer> indexes = new ArrayList<Integer>();
        for (i = 0; i < list.size(); ++i) {
            indexes.add(-1);
        }
        for (i = 0; i < list.size(); ++i) {
            int sortedIndex = Collections.binarySearch(list, byteStrings[i]);
            indexes.set(sortedIndex, i);
        }
        if (((ByteString)list.get(0)).size() == 0) {
            throw new IllegalArgumentException("the empty byte string is not a supported option");
        }
        for (int a = 0; a < list.size(); ++a) {
            ByteString byteString;
            ByteString prefix = (ByteString)list.get(a);
            int b = a + 1;
            while (b < list.size() && (byteString = (ByteString)list.get(b)).startsWith(prefix)) {
                if (byteString.size() == prefix.size()) {
                    throw new IllegalArgumentException("duplicate option: " + byteString);
                }
                if ((Integer)indexes.get(b) > (Integer)indexes.get(a)) {
                    list.remove(b);
                    indexes.remove(b);
                    continue;
                }
                ++b;
            }
        }
        Buffer trieBytes = new Buffer();
        Options.buildTrieRecursive(0L, trieBytes, 0, list, 0, list.size(), indexes);
        int[] trie = new int[Options.intCount(trieBytes)];
        for (int i2 = 0; i2 < trie.length; ++i2) {
            trie[i2] = trieBytes.readInt();
        }
        if (!trieBytes.exhausted()) {
            throw new AssertionError();
        }
        return new Options((ByteString[])byteStrings.clone(), trie);
    }

    private static void buildTrieRecursive(long nodeOffset, Buffer node, int byteStringOffset, List<ByteString> byteStrings, int fromIndex, int toIndex, List<Integer> indexes) {
        if (fromIndex >= toIndex) {
            throw new AssertionError();
        }
        for (int i = fromIndex; i < toIndex; ++i) {
            if (byteStrings.get(i).size() < byteStringOffset) {
                throw new AssertionError();
            }
        }
        ByteString from = byteStrings.get(fromIndex);
        ByteString to = byteStrings.get(toIndex - 1);
        int prefixIndex = -1;
        if (byteStringOffset == from.size()) {
            prefixIndex = indexes.get(fromIndex);
            from = byteStrings.get(++fromIndex);
        }
        if (from.getByte(byteStringOffset) != to.getByte(byteStringOffset)) {
            int selectChoiceCount = 1;
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                if (byteStrings.get(i - 1).getByte(byteStringOffset) == byteStrings.get(i).getByte(byteStringOffset)) continue;
                ++selectChoiceCount;
            }
            long childNodesOffset = nodeOffset + (long)Options.intCount(node) + 2L + (long)(selectChoiceCount * 2);
            node.writeInt(selectChoiceCount);
            node.writeInt(prefixIndex);
            for (int i = fromIndex; i < toIndex; ++i) {
                byte rangeByte = byteStrings.get(i).getByte(byteStringOffset);
                if (i != fromIndex && rangeByte == byteStrings.get(i - 1).getByte(byteStringOffset)) continue;
                node.writeInt(rangeByte & 0xFF);
            }
            Buffer childNodes = new Buffer();
            int rangeStart = fromIndex;
            while (rangeStart < toIndex) {
                byte rangeByte = byteStrings.get(rangeStart).getByte(byteStringOffset);
                int rangeEnd = toIndex;
                for (int i = rangeStart + 1; i < toIndex; ++i) {
                    if (rangeByte == byteStrings.get(i).getByte(byteStringOffset)) continue;
                    rangeEnd = i;
                    break;
                }
                if (rangeStart + 1 == rangeEnd && byteStringOffset + 1 == byteStrings.get(rangeStart).size()) {
                    node.writeInt(indexes.get(rangeStart));
                } else {
                    node.writeInt((int)(-1L * (childNodesOffset + (long)Options.intCount(childNodes))));
                    Options.buildTrieRecursive(childNodesOffset, childNodes, byteStringOffset + 1, byteStrings, rangeStart, rangeEnd, indexes);
                }
                rangeStart = rangeEnd;
            }
            node.write(childNodes, childNodes.size());
        } else {
            int scanByteCount = 0;
            int max = Math.min(from.size(), to.size());
            for (int i = byteStringOffset; i < max && from.getByte(i) == to.getByte(i); ++i) {
                ++scanByteCount;
            }
            long childNodesOffset = nodeOffset + (long)Options.intCount(node) + 2L + (long)scanByteCount + 1L;
            node.writeInt(-scanByteCount);
            node.writeInt(prefixIndex);
            for (int i = byteStringOffset; i < byteStringOffset + scanByteCount; ++i) {
                node.writeInt(from.getByte(i) & 0xFF);
            }
            if (fromIndex + 1 == toIndex) {
                if (byteStringOffset + scanByteCount != byteStrings.get(fromIndex).size()) {
                    throw new AssertionError();
                }
                node.writeInt(indexes.get(fromIndex));
            } else {
                Buffer childNodes = new Buffer();
                node.writeInt((int)(-1L * (childNodesOffset + (long)Options.intCount(childNodes))));
                Options.buildTrieRecursive(childNodesOffset, childNodes, byteStringOffset + scanByteCount, byteStrings, fromIndex, toIndex, indexes);
                node.write(childNodes, childNodes.size());
            }
        }
    }

    @Override
    public ByteString get(int i) {
        return this.byteStrings[i];
    }

    @Override
    public final int size() {
        return this.byteStrings.length;
    }

    private static int intCount(Buffer trieBytes) {
        return (int)(trieBytes.size() / 4L);
    }
}

