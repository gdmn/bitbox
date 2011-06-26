package pl.devsite.bitbox.server.servlets;

import java.io.InputStream;

/**
 *
 * @author dmn
 */
public interface InputProcessor {

    void setRequestStream(InputStream input);

    void setRequestHeader(String requestHeader);
}
