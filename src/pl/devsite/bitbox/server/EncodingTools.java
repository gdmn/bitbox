package pl.devsite.bitbox.server;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.text.Normalizer;
import sun.nio.cs.ThreadLocalCoders;

/**
 *
 * @author dmn
 */
public class EncodingTools {

///////////////////////////////////////////////////
/////////////// from java.net.URI
    private static int decode(char c) {
        if ((c >= '0') && (c <= '9')) {
            return c - '0';
        }
        if ((c >= 'a') && (c <= 'f')) {
            return c - 'a' + 10;
        }
        if ((c >= 'A') && (c <= 'F')) {
            return c - 'A' + 10;
        }
        assert false;
        return -1;
    }

    private static byte decode(char c1, char c2) {
        return (byte) (((decode(c1) & 0xf) << 4) | ((decode(c2) & 0xf) << 0));
    }

    private static String decode(String s) {
        if (s == null) {
            return s;
        }
        int n = s.length();
        if (n == 0) {
            return s;
        }
        if (s.indexOf('%') < 0) {
            return s;
        }

        byte[] ba = new byte[n];
        StringBuffer sb = new StringBuffer(n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = ThreadLocalCoders.decoderFor("UTF-8").onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);

        // This is not horribly efficient, but it will do for now
        char c = s.charAt(0);
        boolean betweenBrackets = false;

        for (int i = 0; i < n;) {
            assert c == s.charAt(i);	// Loop invariant
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false;
            }
            if (c != '%' || betweenBrackets) {
                sb.append(c);
                if (++i >= n) {
                    break;
                }
                c = s.charAt(i);
                continue;
            }
            bb.clear();
            int ui = i;
            for (;;) {
                assert (n - i >= 2);
                bb.put(decode(s.charAt(++i), s.charAt(++i)));
                if (++i >= n) {
                    break;
                }
                c = s.charAt(i);
                if (c != '%') {
                    break;
                }
            }
            bb.flip();
            cb.clear();
            dec.reset();
            CoderResult cr = dec.decode(bb, cb, true);
            assert cr.isUnderflow();
            cr = dec.flush(cb);
            assert cr.isUnderflow();
            sb.append(cb.flip().toString());
        }

        return sb.toString();//.replace("%20", " ");
    }

    private static String encode(String s) {
        int n = s.length();
        if (n == 0) {
            return s;
        }

        // First check whether we actually need to encode
        for (int i = 0;;) {
            char ch = s.charAt(i);
            if (ch >= '\u0080' || ch == 32) {
                break;
            }
            if (++i >= n) {
                return s;
            }
        }

        String ns = Normalizer.normalize(s, Normalizer.Form.NFC);
        ByteBuffer bb = null;
        try {
            bb = ThreadLocalCoders.encoderFor("UTF-8").encode(CharBuffer.wrap(ns));
        } catch (CharacterCodingException x) {
            assert false;
        }

        StringBuffer sb = new StringBuffer();
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80 || b == 32) {
                appendEscape(sb, (byte) b);
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }
    private final static char[] hexDigits = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static void appendEscape(StringBuffer sb, byte b) {
        sb.append('%');
        sb.append(hexDigits[(b >> 4) & 0x0f]);
        sb.append(hexDigits[(b >> 0) & 0x0f]);
    }
/////////////// end of java.net.URI
///////////////////////////////////////////////////

    public static String urlEncodeUTF(String k) {
        return encode(k);
//        try {
//            java.net.URLEncoder.encode(k, "UTF-8");
//        } catch (java.io.UnsupportedEncodingException ex) {
//        }
//        return null;
    }

    public static String urlDecodeUTF(String k) {
        return decode(k);
//        try {
//            return java.net.URLDecoder.decode(k, "UTF-8");
//        } catch (java.io.UnsupportedEncodingException ex) {
//        }
//        return null;
    }

//    public static void main(final String args[]) {
//        String a = "a a";
//        String ae = urlEncodeUTF(a);
//        String ad = urlDecodeUTF(ae);
//        System.out.println(a+"\n"+ae+"\n"+ad);
//    }
}
