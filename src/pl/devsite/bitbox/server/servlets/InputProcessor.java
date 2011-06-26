package pl.devsite.bitbox.server.servlets;

import java.io.InputStream;
import pl.devsite.bitbox.server.HttpHeader;

/**
 *
 * @author dmn
 */
public interface InputProcessor {

    void setRequestStream(InputStream input);

    void setRequestHeader(HttpHeader requestHeader);
}
