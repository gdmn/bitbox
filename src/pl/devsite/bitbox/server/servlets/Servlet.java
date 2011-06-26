package pl.devsite.bitbox.server.servlets;

import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.server.HasHtmlHeaders;

/**
 *
 * @author dmn
 */
public interface Servlet extends Sendable, HasHtmlHeaders, InputProcessor {

}
