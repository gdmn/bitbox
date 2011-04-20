package pl.devsite.log;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author Damian Gorlach
 */
public class CustomFormatter extends Formatter {

	private static Format formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

	public CustomFormatter() {
		super();
	}

	@Override
	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		sb.append('[').append(record.getLevel().getName()).append(']');

		sb.append(' ');

		Date date = new Date(record.getMillis());
		sb.append(formatter.format(date)).append(',');

		sb.append(' ');

		sb.append(formatMessage(record));
		sb.append('\n');

		return sb.toString();
	}
}