package com.github.sett4.dataformat.xlsx;

import java.util.Arrays;

public final class CharTypes {
    protected static final char[] HC = "0123456789ABCDEF".toCharArray();
    protected static final byte[] HB;
    protected static final int[] sInputCodes;
    protected static final int[] sInputCodesUTF8;
    protected static final int[] sInputCodesJsNames;
    protected static final int[] sInputCodesUtf8JsNames;
    protected static final int[] sInputCodesComment;
    protected static final int[] sInputCodesWS;
    protected static final int[] sOutputEscapes128;
    protected static final int[] sHexValues;

    public CharTypes() {
    }

    public static int[] getInputCodeLatin1() {
        return sInputCodes;
    }

    public static int[] getInputCodeUtf8() {
        return sInputCodesUTF8;
    }

    public static int[] getInputCodeLatin1JsNames() {
        return sInputCodesJsNames;
    }

    public static int[] getInputCodeUtf8JsNames() {
        return sInputCodesUtf8JsNames;
    }

    public static int[] getInputCodeComment() {
        return sInputCodesComment;
    }

    public static int[] getInputCodeWS() {
        return sInputCodesWS;
    }

    public static int[] get7BitOutputEscapes() {
        return sOutputEscapes128;
    }

    public static int[] get7BitOutputEscapes(int quoteChar) {
        return quoteChar == 34 ? sOutputEscapes128 : AltEscapes.instance.escapesFor(quoteChar);
    }

    public static int charToHex(int ch) {
        return sHexValues[ch & 255];
    }

    public static char hexToChar(int ch) {
        return HC[ch];
    }

    public static void appendQuoted(StringBuilder sb, String content) {
        int[] escCodes = sOutputEscapes128;
        int escLen = escCodes.length;
        int i = 0;

        for(int len = content.length(); i < len; ++i) {
            char c = content.charAt(i);
            if (c < escLen && escCodes[c] != 0) {
                sb.append('\\');
                int escCode = escCodes[c];
                if (escCode < 0) {
                    sb.append('u');
                    sb.append('0');
                    sb.append('0');
                    sb.append(HC[c >> 4]);
                    sb.append(HC[c & 15]);
                } else {
                    sb.append((char)escCode);
                }
            } else {
                sb.append(c);
            }
        }

    }

    public static char[] copyHexChars() {
        return (char[])((char[])HC.clone());
    }

    public static byte[] copyHexBytes() {
        return (byte[])((byte[])HB.clone());
    }

    static {
        int j = HC.length;
        HB = new byte[j];

        int i;
        for(i = 0; i < j; ++i) {
            HB[i] = (byte)HC[i];
        }

        int[] table = new int[256];

        for(i = 0; i < 32; ++i) {
            table[i] = -1;
        }

        table[34] = 1;
        table[92] = 1;
        sInputCodes = table;
        table = new int[sInputCodes.length];
        System.arraycopy(sInputCodes, 0, table, 0, table.length);

        for(i = 128; i < 256; ++i) {
            byte code;
            if ((i & 224) == 192) {
                code = 2;
            } else if ((i & 240) == 224) {
                code = 3;
            } else if ((i & 248) == 240) {
                code = 4;
            } else {
                code = -1;
            }

            table[i] = code;
        }

        sInputCodesUTF8 = table;
        table = new int[256];
        Arrays.fill(table, -1);

        for(i = 33; i < 256; ++i) {
            if (Character.isJavaIdentifierPart((char)i)) {
                table[i] = 0;
            }
        }

        table[64] = 0;
        table[35] = 0;
        table[42] = 0;
        table[45] = 0;
        table[43] = 0;
        sInputCodesJsNames = table;
        table = new int[256];
        System.arraycopy(sInputCodesJsNames, 0, table, 0, table.length);
        Arrays.fill(table, 128, 128, 0);
        sInputCodesUtf8JsNames = table;
        table = new int[256];
        System.arraycopy(sInputCodesUTF8, 128, table, 128, 128);
        Arrays.fill(table, 0, 32, -1);
        table[9] = 0;
        table[10] = 10;
        table[13] = 13;
        table[42] = 42;
        sInputCodesComment = table;
        table = new int[256];
        System.arraycopy(sInputCodesUTF8, 128, table, 128, 128);
        Arrays.fill(table, 0, 32, -1);
        table[32] = 1;
        table[9] = 1;
        table[10] = 10;
        table[13] = 13;
        table[47] = 47;
        table[35] = 35;
        sInputCodesWS = table;
        table = new int[128];

        for(i = 0; i < 32; ++i) {
            table[i] = -1;
        }

        table[34] = 34;
        table[92] = 92;
        table[8] = 98;
        table[9] = 116;
        table[12] = 102;
        table[10] = 110;
        table[13] = 114;
        sOutputEscapes128 = table;
        sHexValues = new int[256];
        Arrays.fill(sHexValues, -1);

        for(i = 0; i < 10; sHexValues[48 + i] = i++) {
        }

        for(i = 0; i < 6; ++i) {
            sHexValues[97 + i] = 10 + i;
            sHexValues[65 + i] = 10 + i;
        }

    }

    private static class AltEscapes {
        public static final AltEscapes instance = new AltEscapes();
        private int[][] _altEscapes = new int[128][];

        private AltEscapes() {
        }

        public int[] escapesFor(int quoteChar) {
            int[] esc = this._altEscapes[quoteChar];
            if (esc == null) {
                esc = Arrays.copyOf(sOutputEscapes128, 128);
                if (esc[quoteChar] == 0) {
                    esc[quoteChar] = -1;
                }

                this._altEscapes[quoteChar] = esc;
            }

            return esc;
        }
    }
}
