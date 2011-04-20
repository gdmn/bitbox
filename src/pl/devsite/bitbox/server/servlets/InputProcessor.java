/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
