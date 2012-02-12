package pl.devsite.bitbox.sendables;

import java.text.MessageFormat;
import pl.devsite.bitbox.server.BitBoxConfiguration;
import pl.devsite.bitbox.server.HttpHeader;
import pl.devsite.bitbox.server.HttpTools;

/**
 *
 * @author dmn
 */
public enum SendableTemplates {

	SIMPLE("<h1>$code$ <small>$codemessage$</small></h1><hr><i>$server$</i>"),
	SIMPLEMESSAGE("<h1>$code$ <small>$codemessage$</small></h1><p>{0}</p><hr><i>$server$</i>");
	static private BitBoxConfiguration config = BitBoxConfiguration.getInstance();
	private String template;

	private SendableTemplates(String template) {
		this.template = template;
	}

	public Sendable create(HttpHeader header, String... arguments) {
		String text = replaceDolars(header, template);
		text = MessageFormat.format(text, arguments);
		text = outlineWithBody(text);
		SendableString result = new SendableString(null, header.getHttpResponseCode() == 200 ? "Information" : "Error", HttpTools.CONTENTTYPE_TEXT_HTML, text);
		return result;
	}

	private static String outlineWithBody(String value) {
		return MessageFormat.format("<html><body>\n{0}\n</body></html>", value);
	}

	private static String replaceDolars(HttpHeader header, String text) {
		text = text.replace("$code$", "" + header.getHttpResponseCode());
		String codeMsg = HttpTools.getHttpCodes().get(header.getHttpResponseCode());
		if (codeMsg == null) {
			codeMsg = "";
		}
		text = text.replace("$codemessage$", codeMsg);
		text = text.replace("$server$", config.getProperty(BitBoxConfiguration.PROPERTY_NAME));
		return text;
	}

}
