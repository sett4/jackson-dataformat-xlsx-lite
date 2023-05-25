package com.github.sett4.dataformat.xlsx;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Objects;

public class ContentReference implements Serializable {
    private static final long serialVersionUID = 1L;
    protected static final ContentReference UNKNOWN_CONTENT = new ContentReference(false, null);
    public static final int DEFAULT_MAX_CONTENT_SNIPPET = 500;
    protected final transient Object _rawContent;
    protected final int _offset;
    protected final int _length;
    protected final boolean _isContentTextual;

    protected ContentReference(boolean isContentTextual, Object rawContent) {
        this(isContentTextual, rawContent, -1, -1);
    }

    protected ContentReference(boolean isContentTextual, Object rawContent, int offset, int length) {
        this._isContentTextual = isContentTextual;
        this._rawContent = rawContent;
        this._offset = offset;
        this._length = length;
    }

    public static ContentReference unknown() {
        return UNKNOWN_CONTENT;
    }

    public static ContentReference construct(boolean isContentTextual, Object rawContent) {
        return new ContentReference(isContentTextual, rawContent);
    }

    public static ContentReference construct(boolean isContentTextual, Object rawContent, int offset, int length) {
        return new ContentReference(isContentTextual, rawContent, offset, length);
    }

    public static ContentReference rawReference(boolean isContentTextual, Object rawContent) {
        return rawContent instanceof ContentReference ? (ContentReference)rawContent : new ContentReference(isContentTextual, rawContent);
    }

    public static ContentReference rawReference(Object rawContent) {
        return rawReference(false, rawContent);
    }

    private void readObject(ObjectInputStream in) throws IOException {
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
    }

    protected Object readResolve() {
        return UNKNOWN_CONTENT;
    }

    public boolean hasTextualContent() {
        return this._isContentTextual;
    }

    public Object getRawContent() {
        return this._rawContent;
    }

    public int contentOffset() {
        return this._offset;
    }

    public int contentLength() {
        return this._length;
    }

    protected int maxContentSnippetLength() {
        return 500;
    }

    public String buildSourceDescription() {
        return this.appendSourceDescription(new StringBuilder(200)).toString();
    }

    public StringBuilder appendSourceDescription(StringBuilder sb) {
        Object srcRef = this.getRawContent();
        if (srcRef == null) {
            sb.append("UNKNOWN");
            return sb;
        } else {
            Class<?> srcType = srcRef instanceof Class ? (Class)srcRef : srcRef.getClass();
            String tn = srcType.getName();
            if (tn.startsWith("java.")) {
                tn = srcType.getSimpleName();
            } else if (srcRef instanceof byte[]) {
                tn = "byte[]";
            } else if (srcRef instanceof char[]) {
                tn = "char[]";
            }

            sb.append('(').append(tn).append(')');
            if (this.hasTextualContent()) {
                String unitStr = " chars";
                int maxLen = this.maxContentSnippetLength();
                int[] offsets = new int[]{this.contentOffset(), this.contentLength()};
                String trimmed;
                if (srcRef instanceof CharSequence) {
                    trimmed = this._truncate((CharSequence)srcRef, offsets, maxLen);
                } else if (srcRef instanceof char[]) {
                    trimmed = this._truncate((char[]) srcRef, offsets, maxLen);
                } else if (srcRef instanceof byte[]) {
                    trimmed = this._truncate((byte[]) srcRef, offsets, maxLen);
                    unitStr = " bytes";
                } else {
                    trimmed = null;
                }

                if (trimmed != null) {
                    this._append(sb, trimmed);
                    if (offsets[1] > maxLen) {
                        sb.append("[truncated ").append(offsets[1] - maxLen).append(unitStr).append(']');
                    }
                }
            } else if (srcRef instanceof byte[]) {
                int length = this.contentLength();
                if (length < 0) {
                    length = ((byte[]) srcRef).length;
                }

                sb.append('[').append(length).append(" bytes]");
            }

            return sb;
        }
    }

    protected String _truncate(CharSequence cs, int[] offsets, int maxSnippetLen) {
        this._truncateOffsets(offsets, cs.length());
        int start = offsets[0];
        int length = Math.min(offsets[1], maxSnippetLen);
        return cs.subSequence(start, start + length).toString();
    }

    protected String _truncate(char[] cs, int[] offsets, int maxSnippetLen) {
        this._truncateOffsets(offsets, cs.length);
        int start = offsets[0];
        int length = Math.min(offsets[1], maxSnippetLen);
        return new String(cs, start, length);
    }

    protected String _truncate(byte[] b, int[] offsets, int maxSnippetLen) {
        this._truncateOffsets(offsets, b.length);
        int start = offsets[0];
        int length = Math.min(offsets[1], maxSnippetLen);
        return new String(b, start, length, Charset.forName("UTF-8"));
    }

    protected void _truncateOffsets(int[] offsets, int actualLength) {
        int start = offsets[0];
        if (start < 0) {
            start = 0;
        } else if (start >= actualLength) {
            start = actualLength;
        }

        offsets[0] = start;
        int length = offsets[1];
        int maxLength = actualLength - start;
        if (length < 0 || length > maxLength) {
            offsets[1] = maxLength;
        }

    }

    protected int _append(StringBuilder sb, String content) {
        sb.append('"');
        int i = 0;

        for(int end = content.length(); i < end; ++i) {
            char ch = content.charAt(i);
            if (!Character.isISOControl(ch) || !this._appendEscaped(sb, ch)) {
                sb.append(ch);
            }
        }

        sb.append('"');
        return content.length();
    }

    protected boolean _appendEscaped(StringBuilder sb, int ctrlChar) {
        if (ctrlChar != 13 && ctrlChar != 10) {
            sb.append('\\');
            sb.append('u');
            sb.append(CharTypes.hexToChar(ctrlChar >> 12 & 15));
            sb.append(CharTypes.hexToChar(ctrlChar >> 8 & 15));
            sb.append(CharTypes.hexToChar(ctrlChar >> 4 & 15));
            sb.append(CharTypes.hexToChar(ctrlChar & 15));
            return true;
        } else {
            return false;
        }
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other == null) {
            return false;
        } else if (!(other instanceof ContentReference)) {
            return false;
        } else {
            ContentReference otherSrc = (ContentReference)other;
            return this._rawContent == otherSrc._rawContent;
        }
    }

    public int hashCode() {
        return Objects.hashCode(this._rawContent);
    }
}

