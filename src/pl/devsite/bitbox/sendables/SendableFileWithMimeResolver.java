package pl.devsite.bitbox.sendables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 *
 * @author dmn
 */
public class SendableFileWithMimeResolver extends SendableFile {

    public SendableFileWithMimeResolver(Sendable parent, File file) {
        super(parent, file);
    }

    @Override
    public String getMimeType() {
        if (file.isDirectory()) {
            return "text/html";
        } else {
            final String defaultType = "application/octet-stream";
            String fileName = file.getName();
            int dotPos = fileName.lastIndexOf(".");
            if (dotPos < 0) {
                return defaultType;
            }
            String fileExt = fileName.substring(dotPos + 1).toLowerCase();
            return resolveFileExt(fileExt);
        }
    }

    private static boolean contains(String what, String... where) {
        for (String s : where) {
            if (what.equals(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Sendable getInstance(Sendable parent, File file) {
        return new SendableFileWithMimeResolver(parent, file);
    }

    static class Generator {

        /**
         * Generate code for mime type resolver.
         * File format:
         * (..)
         * bin 	application/octet-stream
         * bmp 	image/bmp
         * (..)
         * example mime type list: http://www.feedforall.com/mime-types.htm
         * @param
         */
        public static void generator1() throws FileNotFoundException {
            Scanner s = null;
            try {
                s = new Scanner(new BufferedReader(new FileReader("c:/mime.txt")));
                HashMap<String, String> map = new HashMap<String, String>();
                String ext = null;
                while (s.hasNext()) {
                    String piece = s.next();
                    if (ext == null) {
                        ext = piece;
                    } else {
                        String extensions = map.get(piece);
                        if (extensions == null) {
                            extensions = "";
                        }
                        extensions = extensions + " " + ext;
                        map.put(piece, extensions);
                        ext = null;
                    }
                }
                ArrayList<String> mimes = new ArrayList<String>(map.size());
                for (Entry<String, String> kk : map.entrySet()) {
                    String entry = kk.getKey() + " " + kk.getValue();
                    mimes.add(entry);
                }
                Object[] mimesTable = mimes.toArray();
                Arrays.sort(mimesTable);
                for (Object w : mimesTable) {
                    //System.out.println(""+w);
                    String[] splitted = w.toString().trim().replace("  ", " ").split(" ");
                    System.out.print("        ");
                    System.out.print("if (contains(fileExt");
                    for (int i = 1; i < splitted.length; i++) {
                        System.out.print(", \"" + splitted[i] + "\"");
                    }
                    System.out.print(")) { ");
                    System.out.print("return \"" + splitted[0] + "\";");
                    System.out.println(" }");
                }
            } finally {
                if (s != null) {
                    s.close();
                }
            }
        }

        /**
         * Generate code for mime type resolver.
         * File format:
         * (..)
         * mime!application/smil!extensions = smi,smil
         * mime!application/vnd.mozilla.xul+xml!extensions = xul
         * (..)
         * example mime type list: http://www.feedforall.com/mime-types.htm
         * @param
         */
        public static void generator2() throws FileNotFoundException, IOException {
            BufferedReader s = null;
            try {
                s = new BufferedReader(new FileReader("c:/mime2.txt"));
                String line;
                while ((line = s.readLine()) != null) {
                    if (line.startsWith("mime!")) {
                        int divider = line.indexOf("!", 5);
                        String type = line.substring(5, divider);
                        String ext = line.substring(divider + 1);
                        if (ext.startsWith("extensions")) {
                            int divider2 = ext.indexOf("=");
                            ext = ext.substring(divider2 + 1);
                            String[] splitted = ext.toString().trim().replace(" ", "").split(",");
                            System.out.print("        ");
                            System.out.print("if (contains(fileExt");
                            for (int i = 0; i < splitted.length; i++) {
                                System.out.print(", \"" + splitted[i] + "\"");
                            }
                            System.out.print(")) { ");
                            System.out.print("return \"" + type + "\";");
                            System.out.println(" }");
                        }
                    }
                }
            } finally {
                if (s != null) {
                    s.close();
                }
            }
        }

        public static void main(String args[]) throws FileNotFoundException, IOException {
            generator2();
        }
    }

    public static String resolveFileExt(String fileExt) {
        final String defaultType = "application/octet-stream";
        if (contains(fileExt, "ics", "icz")) { return "text/calendar"; }
        if (contains(fileExt, "csv")) { return "text/comma-separated-values"; }
        if (contains(fileExt, "css")) { return "text/css"; }
        if (contains(fileExt, "323")) { return "text/h323"; }
        if (contains(fileExt, "html", "htm", "shtml")) { return "text/html"; }
        if (contains(fileExt, "uls")) { return "text/iuls"; }
        if (contains(fileExt, "mml")) { return "text/mathml"; }
        if (contains(fileExt, "asc", "txt", "text", "diff", "pot")) { return "text/plain"; }
        if (contains(fileExt, "rtx")) { return "text/richtext"; }
        if (contains(fileExt, "rtf")) { return "text/rtf"; }
        if (contains(fileExt, "sct", "wsc")) { return "text/scriptlet"; }
        if (contains(fileExt, "tsv")) { return "text/tab-separated-values"; }
        if (contains(fileExt, "jad")) { return "text/vnd.sun.j2me.app-descriptor"; }
        if (contains(fileExt, "wml")) { return "text/vnd.wap.wml"; }
        if (contains(fileExt, "wmls")) { return "text/vnd.wap.wmlscript"; }
        if (contains(fileExt, "boo")) { return "text/x-boo"; }
        if (contains(fileExt, "h++", "hpp", "hxx", "hh")) { return "text/x-c++hdr"; }
        if (contains(fileExt, "c++", "cpp", "cxx", "cc")) { return "text/x-c++src"; }
        if (contains(fileExt, "h")) { return "text/x-chdr"; }
        if (contains(fileExt, "csh")) { return "text/x-csh"; }
        if (contains(fileExt, "c")) { return "text/x-csrc"; }
        if (contains(fileExt, "d")) { return "text/x-dsrc"; }
        if (contains(fileExt, "hs")) { return "text/x-haskell"; }
        if (contains(fileExt, "java")) { return "text/x-java"; }
        if (contains(fileExt, "lhs")) { return "text/x-literate-haskell"; }
        if (contains(fileExt, "moc")) { return "text/x-moc"; }
        if (contains(fileExt, "p", "pas")) { return "text/x-pascal"; }
        if (contains(fileExt, "gcd")) { return "text/x-pcs-gcd"; }
        if (contains(fileExt, "pl", "pm")) { return "text/x-perl"; }
        if (contains(fileExt, "py")) { return "text/x-python"; }
        if (contains(fileExt, "etx")) { return "text/x-setext"; }
        if (contains(fileExt, "sh")) { return "text/x-sh"; }
        if (contains(fileExt, "tcl", "tk")) { return "text/x-tcl"; }
        if (contains(fileExt, "tex", "ltx", "sty", "cls")) { return "text/x-tex"; }
        if (contains(fileExt, "vcs")) { return "text/x-vcalendar"; }
        if (contains(fileExt, "vcf")) { return "text/x-vcard"; }
        if (contains(fileExt, "gif")) { return "image/gif"; }
        if (contains(fileExt, "jpeg", "jpg", "jpe")) { return "image/jpeg"; }
        if (contains(fileExt, "pcx")) { return "image/pcx"; }
        if (contains(fileExt, "png")) { return "image/png"; }
        if (contains(fileExt, "svg", "svgz")) { return "image/svg+xml"; }
        if (contains(fileExt, "tiff", "tif")) { return "image/tiff"; }
        if (contains(fileExt, "djvu", "djv")) { return "image/vnd.djvu"; }
        if (contains(fileExt, "wbmp")) { return "image/vnd.wap.wbmp"; }
        if (contains(fileExt, "ico")) { return "image/x-icon"; }
        if (contains(fileExt, "bmp")) { return "image/x-ms-bmp"; }
        if (contains(fileExt, "psd")) { return "image/x-photoshop"; }
        if (contains(fileExt, "pnm")) { return "image/x-portable-anymap"; }
        if (contains(fileExt, "pbm")) { return "image/x-portable-bitmap"; }
        if (contains(fileExt, "pgm")) { return "image/x-portable-graymap"; }
        if (contains(fileExt, "ppm")) { return "image/x-portable-pixmap"; }
        if (contains(fileExt, "xbm")) { return "image/x-xbitmap"; }
        if (contains(fileExt, "xpm")) { return "image/x-xpixmap"; }
        if (contains(fileExt, "xwd")) { return "image/x-xwindowdump"; }
        if (contains(fileExt, "bz2")) { return "application/bzip2"; }
        if (contains(fileExt, "gz")) { return "application/gzip"; }
        if (contains(fileExt, "hta")) { return "application/hta"; }
        if (contains(fileExt, "jar")) { return "application/java-archive"; }
        if (contains(fileExt, "ser")) { return "application/java-serialized-object"; }
        if (contains(fileExt, "class")) { return "application/java-vm"; }
        if (contains(fileExt, "json")) { return "application/json"; }
        if (contains(fileExt, "hqx")) { return "application/mac-binhex40"; }
        if (contains(fileExt, "mdb")) { return "application/msaccess"; }
        if (contains(fileExt, "doc", "dot")) { return "application/msword"; }
        if (contains(fileExt, "bin")) { return "application/octet-stream"; }
        if (contains(fileExt, "ace")) { return "application/octetstream"; }
        if (contains(fileExt, "oda")) { return "application/oda"; }
        if (contains(fileExt, "ogx")) { return "application/ogg"; }
        if (contains(fileExt, "pdf")) { return "application/pdf"; }
        if (contains(fileExt, "key")) { return "application/pgp-keys"; }
        if (contains(fileExt, "pgp")) { return "application/pgp-signature"; }
        if (contains(fileExt, "prf")) { return "application/pics-rules"; }
        if (contains(fileExt, "ps", "ai", "eps")) { return "application/postscript"; }
        if (contains(fileExt, "rar")) { return "application/rar"; }
        if (contains(fileExt, "rdf")) { return "application/rdf+xml"; }
        if (contains(fileExt, "rss")) { return "application/rss+xml"; }
        if (contains(fileExt, "smi", "smil")) { return "application/smil"; }
        if (contains(fileExt, "xul")) { return "application/vnd.mozilla.xul+xml"; }
        if (contains(fileExt, "xls", "xlb", "xlt")) { return "application/vnd.ms-excel"; }
        if (contains(fileExt, "cat")) { return "application/vnd.ms-pki.seccat"; }
        if (contains(fileExt, "stl")) { return "application/vnd.ms-pki.stl"; }
        if (contains(fileExt, "ppt", "pps")) { return "application/vnd.ms-powerpoint"; }
        if (contains(fileExt, "odc")) { return "application/vnd.oasis.opendocument.chart"; }
        if (contains(fileExt, "odb")) { return "application/vnd.oasis.opendocument.database"; }
        if (contains(fileExt, "odf")) { return "application/vnd.oasis.opendocument.formula"; }
        if (contains(fileExt, "odg")) { return "application/vnd.oasis.opendocument.graphics"; }
        if (contains(fileExt, "odi")) { return "application/vnd.oasis.opendocument.image"; }
        if (contains(fileExt, "odp")) { return "application/vnd.oasis.opendocument.presentation"; }
        if (contains(fileExt, "ods")) { return "application/vnd.oasis.opendocument.spreadsheet"; }
        if (contains(fileExt, "odt")) { return "application/vnd.oasis.opendocument.text"; }
        if (contains(fileExt, "odm")) { return "application/vnd.oasis.opendocument.text-master"; }
        if (contains(fileExt, "oth")) { return "application/vnd.oasis.opendocument.text-web"; }
        if (contains(fileExt, "p5i")) { return "application/vnd.pkg5.info"; }
        if (contains(fileExt, "vsd")) { return "application/vnd.visio"; }
        if (contains(fileExt, "wbxml")) { return "application/vnd.wap.wbxml"; }
        if (contains(fileExt, "wmlc")) { return "application/vnd.wap.wmlc"; }
        if (contains(fileExt, "wmlsc")) { return "application/vnd.wap.wmlscriptc"; }
        if (contains(fileExt, "abw")) { return "application/x-abiword"; }
        if (contains(fileExt, "dmg")) { return "application/x-apple-diskimage"; }
        if (contains(fileExt, "bcpio")) { return "application/x-bcpio"; }
        if (contains(fileExt, "torrent")) { return "application/x-bittorrent"; }
        if (contains(fileExt, "cdf")) { return "application/x-cdf"; }
        if (contains(fileExt, "cpio")) { return "application/x-cpio"; }
        if (contains(fileExt, "csh")) { return "application/x-csh"; }
        if (contains(fileExt, "deb", "udeb")) { return "application/x-debian-package"; }
        if (contains(fileExt, "dcr", "dir", "dxr")) { return "application/x-director"; }
        if (contains(fileExt, "dvi")) { return "application/x-dvi"; }
        if (contains(fileExt, "flac")) { return "application/x-flac"; }
        if (contains(fileExt, "pfa", "pfb", "gsf", "pcf", "pcf.Z")) { return "application/x-font"; }
        if (contains(fileExt, "mm")) { return "application/x-freemind"; }
        if (contains(fileExt, "gnumeric")) { return "application/x-gnumeric"; }
        if (contains(fileExt, "gtar", "tgz", "taz")) { return "application/x-gtar"; }
        if (contains(fileExt, "gz", "tgz")) { return "application/x-gzip"; }
        if (contains(fileExt, "phtml", "pht", "php")) { return "application/x-httpd-php"; }
        if (contains(fileExt, "phps")) { return "application/x-httpd-php-source"; }
        if (contains(fileExt, "php3")) { return "application/x-httpd-php3"; }
        if (contains(fileExt, "php3p")) { return "application/x-httpd-php3-preprocessed"; }
        if (contains(fileExt, "php4")) { return "application/x-httpd-php4"; }
        if (contains(fileExt, "ins", "isp")) { return "application/x-internet-signup"; }
        if (contains(fileExt, "iii")) { return "application/x-iphone"; }
        if (contains(fileExt, "iso")) { return "application/x-iso9660-image"; }
        if (contains(fileExt, "jnlp")) { return "application/x-java-jnlp-file"; }
        if (contains(fileExt, "js")) { return "application/x-javascript"; }
        if (contains(fileExt, "chrt")) { return "application/x-kchart"; }
        if (contains(fileExt, "kil")) { return "application/x-killustrator"; }
        if (contains(fileExt, "skp", "skd", "skt", "skm")) { return "application/x-koan"; }
        if (contains(fileExt, "kpr", "kpt")) { return "application/x-kpresenter"; }
        if (contains(fileExt, "ksp")) { return "application/x-kspread"; }
        if (contains(fileExt, "kwd", "kwt")) { return "application/x-kword"; }
        if (contains(fileExt, "latex")) { return "application/x-latex"; }
        if (contains(fileExt, "lha")) { return "application/x-lha"; }
        if (contains(fileExt, "lzh")) { return "application/x-lzh"; }
        if (contains(fileExt, "lzx")) { return "application/x-lzx"; }
        if (contains(fileExt, "wmd")) { return "application/x-ms-wmd"; }
        if (contains(fileExt, "wmz")) { return "application/x-ms-wmz"; }
        if (contains(fileExt, "com", "exe", "bat", "dll")) { return "application/x-msdos-program"; }
        if (contains(fileExt, "msi")) { return "application/x-msi"; }
        if (contains(fileExt, "nc")) { return "application/x-netcdf"; }
        if (contains(fileExt, "pac")) { return "application/x-ns-proxy-autoconfig"; }
        if (contains(fileExt, "nwc")) { return "application/x-nwc"; }
        if (contains(fileExt, "o")) { return "application/x-object"; }
        if (contains(fileExt, "oza")) { return "application/x-oz-application"; }
        if (contains(fileExt, "p7r")) { return "application/x-pkcs7-certreqresp"; }
        if (contains(fileExt, "crl")) { return "application/x-pkcs7-crl"; }
        if (contains(fileExt, "pyc", "pyo")) { return "application/x-python-code"; }
        if (contains(fileExt, "qtl")) { return "application/x-quicktimeplayer"; }
        if (contains(fileExt, "rpm")) { return "application/x-redhat-package-manager"; }
        if (contains(fileExt, "sh")) { return "application/x-sh"; }
        if (contains(fileExt, "shar")) { return "application/x-shar"; }
        if (contains(fileExt, "swf", "swfl")) { return "application/x-shockwave-flash"; }
        if (contains(fileExt, "sit", "sea")) { return "application/x-stuffit"; }
        if (contains(fileExt, "sv4cpio")) { return "application/x-sv4cpio"; }
        if (contains(fileExt, "sv4crc")) { return "application/x-sv4crc"; }
        if (contains(fileExt, "tar")) { return "application/x-tar"; }
        if (contains(fileExt, "tcl")) { return "application/x-tcl"; }
        if (contains(fileExt, "pk")) { return "application/x-tex-pk"; }
        if (contains(fileExt, "texinfo", "texi")) { return "application/x-texinfo"; }
        if (contains(fileExt, "~", "bak", "old", "sik")) { return "application/x-trash"; }
        if (contains(fileExt, "t", "tr", "roff")) { return "application/x-troff"; }
        if (contains(fileExt, "man")) { return "application/x-troff-man"; }
        if (contains(fileExt, "me")) { return "application/x-troff-me"; }
        if (contains(fileExt, "ms")) { return "application/x-troff-ms"; }
        if (contains(fileExt, "ustar")) { return "application/x-ustar"; }
        if (contains(fileExt, "crt")) { return "application/x-x509-ca-cert"; }
        if (contains(fileExt, "xcf")) { return "application/x-xcf"; }
        if (contains(fileExt, "fig")) { return "application/x-xfig"; }
        if (contains(fileExt, "xpi")) { return "application/x-xpinstall"; }
        if (contains(fileExt, "xhtml", "xht")) { return "application/xhtml+xml"; }
        if (contains(fileExt, "xml", "xsl")) { return "application/xml"; }
        if (contains(fileExt, "zip")) { return "application/zip"; }
        if (contains(fileExt, "au", "snd")) { return "audio/basic"; }
        if (contains(fileExt, "mid", "midi", "kar")) { return "audio/midi"; }
        if (contains(fileExt, "mpga", "mpega", "mp2", "mp3", "m4a")) { return "audio/mpeg"; }
        if (contains(fileExt, "ogg", "oga")) { return "audio/ogg"; }
        if (contains(fileExt, "sid")) { return "audio/prs.sid"; }
        if (contains(fileExt, "aif", "aiff", "aifc")) { return "audio/x-aiff"; }
        if (contains(fileExt, "gsm")) { return "audio/x-gsm"; }
        if (contains(fileExt, "m3u")) { return "audio/x-mpegurl"; }
        if (contains(fileExt, "wax")) { return "audio/x-ms-wax"; }
        if (contains(fileExt, "wma")) { return "audio/x-ms-wma"; }
        if (contains(fileExt, "ra", "rm", "ram")) { return "audio/x-pn-realaudio"; }
        if (contains(fileExt, "ra")) { return "audio/x-realaudio"; }
        if (contains(fileExt, "pls")) { return "audio/x-scpls"; }
        if (contains(fileExt, "sd2")) { return "audio/x-sd2"; }
        if (contains(fileExt, "wav")) { return "audio/x-wav"; }
        if (contains(fileExt, "dl")) { return "video/dl"; }
        if (contains(fileExt, "dif", "dv")) { return "video/dv"; }
        if (contains(fileExt, "fli")) { return "video/fli"; }
        if (contains(fileExt, "gl")) { return "video/gl"; }
        if (contains(fileExt, "mp4")) { return "video/mp4"; }
        if (contains(fileExt, "mpeg", "mpg", "mpe")) { return "video/mpeg"; }
        if (contains(fileExt, "ogv")) { return "video/ogg"; }
        if (contains(fileExt, "qt", "mov")) { return "video/quicktime"; }
        if (contains(fileExt, "mxu")) { return "video/vnd.mpegurl"; }
        if (contains(fileExt, "flv")) { return "video/x-flv"; }
        if (contains(fileExt, "lsf", "lsx")) { return "video/x-la-asf"; }
        if (contains(fileExt, "mng")) { return "video/x-mng"; }
        if (contains(fileExt, "asf", "asx")) { return "video/x-ms-asf"; }
        if (contains(fileExt, "wm")) { return "video/x-ms-wm"; }
        if (contains(fileExt, "wmv")) { return "video/x-ms-wmv"; }
        if (contains(fileExt, "wmx")) { return "video/x-ms-wmx"; }
        if (contains(fileExt, "wvx")) { return "video/x-ms-wvx"; }
        if (contains(fileExt, "avi")) { return "video/x-msvideo"; }
        if (contains(fileExt, "movie")) { return "video/x-sgi-movie"; }
        if (contains(fileExt, "ice")) { return "x-conference/x-cooltalk"; }
        if (contains(fileExt, "vrm", "vrml", "wrl")) { return "x-world/x-vrml"; }
        return defaultType;
    }
}
