package pl.devsite.bitbox.server;

import pl.devsite.bitbox.sendables.Sendable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

public class HtmlLister extends InputStream {

    private Sendable sendable;
    private Comparator<Sendable> comparator;

    public HtmlLister(Sendable sendable) {
        super();
        this.sendable = sendable;
        comparator = new Comparator<Sendable>() {

            @Override
            public int compare(Sendable f1, Sendable f2) {
                boolean children1 = f1.hasChildren();
                boolean children2 = f2.hasChildren();
                return (children1 ^ children2) ? (children1 ? -1 : 1) : f1.toString().toLowerCase().compareTo(f2.toString().toLowerCase());
            }
        };
    }

    public String getHtmlList(Sendable... elements) {
        if (elements == null) {
            return null;
        }
        Arrays.sort(elements, comparator);
        StringBuilder listBuffer = new StringBuilder();
        listBuffer.append("<ul class=\"ls\">");
        for (Sendable f : elements) {
            if ((f.getAttributes() & Sendable.ATTR_HIDDEN) == 0) {
                listBuffer.append("<li>");
                boolean dir = f.hasChildren();
                //String href = EncodingTools.urlEncodeUTF(f.toString()) + (dir ? "/" : "");
                String href = f.toString() + (dir ? "/" : "");
                String s = dir ? "[" + f.toString() + "]" : f.toString();
                listBuffer.append(String.format("<a class=\"" + (dir ? "dir" : "file") + "\" href=\"%1$s\">%2$s", href, s));
                long len = f.getContentLength();
                if (len > 0) {
                    listBuffer.append(" <span class=\"size\">(" + String.format("%.02f MB", (double) len / (1024 * 1024)) + ")</span>");
                }
                listBuffer.append("</a></li>");
                listBuffer.append("\n");
            }
        }
        listBuffer.append("</ul>");
        return listBuffer.toString();
    }

    private String getHtmlList() {
        Sendable[] elements = sendable.getChildren();
        return getHtmlList(elements);
    }
    byte[] htmlListBuffer = null;
    int htmlListBufferPos = 0;

    @Override
    public int read() throws IOException {
        if (htmlListBuffer == null) {
            htmlListBuffer = getHtmlList().getBytes("UTF-8");
        }
        if (htmlListBufferPos >= htmlListBuffer.length) {
            return -1;
        } else {
            return htmlListBuffer[htmlListBufferPos++];
        }
    }
}
