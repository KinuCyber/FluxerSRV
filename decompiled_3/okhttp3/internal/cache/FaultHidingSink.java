/*
 * Decompiled with CFR 0.152.
 */
package okhttp3.internal.cache;

import java.io.IOException;
import okio.Buffer;
import okio.ForwardingSink;
import okio.Sink;

class FaultHidingSink
extends ForwardingSink {
    private boolean hasErrors;

    FaultHidingSink(Sink delegate) {
        super(delegate);
    }

    @Override
    public void write(Buffer source, long byteCount) throws IOException {
        if (this.hasErrors) {
            source.skip(byteCount);
            return;
        }
        try {
            super.write(source, byteCount);
        }
        catch (IOException e) {
            this.hasErrors = true;
            this.onException(e);
        }
    }

    @Override
    public void flush() throws IOException {
        if (this.hasErrors) {
            return;
        }
        try {
            super.flush();
        }
        catch (IOException e) {
            this.hasErrors = true;
            this.onException(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (this.hasErrors) {
            return;
        }
        try {
            super.close();
        }
        catch (IOException e) {
            this.hasErrors = true;
            this.onException(e);
        }
    }

    protected void onException(IOException e) {
    }
}

