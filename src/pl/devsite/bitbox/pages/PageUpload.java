package pl.devsite.bitbox.pages;

import pl.devsite.bitbox.sendables.SendableString;
import pl.devsite.bitbox.sendables.Sendable;
import static pl.devsite.bitbox.server.HttpTools.CONTENTTYPE_TEXT_HTML;

/**
 *
 * @author dmn
 */
public class PageUpload extends SendableString {

    private static final String data =
            "<h3>Upload</h3>" +
            "<p>The browser will not display any progress bar. Please be patient.<br/>When done, a message will be displayed.</p>" +
            "<form action=\"/\" method=\"post\" enctype=\"multipart/form-data\">" +
            "<p align=\"center\"><input type=\"file\" name=\"nazwa\" />" +
            "</p>" +
            "<p align=\"center\"><input type=\"reset\" value=\"Clear\" />" +
            "&nbsp;"+
            "<input type=\"submit\" value=\"Send\" />" +
            "</p></form>" +
            "";


    public PageUpload(Sendable parent, String name) {
        super(parent, name, CONTENTTYPE_TEXT_HTML, data);
    }

    @Override
    public long getContentLength() {
        return -1;
    }

    @Override
    public int getAttributes() {
        return super.getAttributes() | Sendable.ATTR_HIDDEN;
    }

}
