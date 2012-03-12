package pl.devsite.bitbox.server.renderers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Logger;
import pl.devsite.bigbitbox.system.MusicEncoder;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableFile;
import pl.devsite.bitbox.sendables.SendableFileWithMimeResolver;
import pl.devsite.bitbox.server.BitBoxConfiguration;
import pl.devsite.bitbox.server.RequestContext;

/**
 *
 * @author dmn
 */
public class IcyRenderer extends Renderer {

	private static final Logger logger = Logger.getLogger(IcyRenderer.class.getName());
	public static final int CHUNK_SIZE = 1024 * 16;

	@Override
	public Object execute() throws Exception {
		//super.send();
		sendHeader();
		if (context.getResponseStream() != null) {
			sendIcyStream();
		}
		return null;
	}

	private void sendIcyStreamTitle(String title, int chunkSize) throws IOException {
		byte[] fileBuffer = new byte[chunkSize];
		Arrays.fill(fileBuffer, (byte) 0);
		String s = "StreamTitle='" + title + "';";
		int dlugosc = 1 + s.length() / 16;
		fileBuffer[0] = (byte) dlugosc;
		for (int i = 0; i < s.length(); i++) {
			fileBuffer[i + 1] = (byte) s.charAt(i);
		}
		context.getClientOut().write(fileBuffer, 0, dlugosc * 16 + 1);
	}

	private void sendIcyStream() throws IOException {
		int n;
		boolean sendHeader = false;
		InputStream in = context.getResponseStream();
		Sendable sendable = context.getSendableResponse();
		InputStream encodedStream = null;
		MusicEncoder encoder = null;
		String name = sendable.toString();
		boolean oggEncoder = "oggenc".equals(BitBoxConfiguration.getInstance().getProperty(BitBoxConfiguration.PROPERTY_TOOLS_ENCODER));
		if (sendable instanceof SendableFile) {
			SendableFile sf = (SendableFile) sendable;
			try {
				encoder = new MusicEncoder();
				SendableFileWithMimeResolver sfm = null;
				if (sendable instanceof SendableFileWithMimeResolver) {
					sfm = (SendableFileWithMimeResolver) sendable;
				}
				encodedStream = sfm == null ? encoder.encode(sf.getFile().getCanonicalPath())
						: encoder.encode(sf.getFile().getCanonicalPath(), sfm.getMetadata(), null);
				if (encodedStream != null) {
					in = encodedStream;
				}
			} catch (IOException e) {
				encoder = null;
				logger.warning("Could not initialize MusicEncoder: " + e.getMessage());
			}
		}
		if (sendable instanceof SendableFileWithMimeResolver) {
			SendableFileWithMimeResolver sf = (SendableFileWithMimeResolver) sendable;
			StringBuilder nameBuilder = new StringBuilder();
			String album = sf.getMetadataValue(SendableFileWithMimeResolver.AUDIO_HTTP_HEADER_PREFIX + "Album");
			String artist = sf.getMetadataValue(SendableFileWithMimeResolver.AUDIO_HTTP_HEADER_PREFIX + "Artist");
			String title = sf.getMetadataValue(SendableFileWithMimeResolver.AUDIO_HTTP_HEADER_PREFIX + "Title");
			String length = sf.getMetadataValue(SendableFileWithMimeResolver.AUDIO_HTTP_HEADER_PREFIX + "Length");
			if (artist != null) {
				nameBuilder.append(artist).append(' ');
			}
			if (album != null) {
				nameBuilder.append("[").append(album).append("] ");
			}
			if (title != null) {
				if (nameBuilder.length() > 0) {
					nameBuilder.append("- ");
				}
				nameBuilder.append(title).append(' ');
			}
			if (nameBuilder.length() > 0) {
				name = nameBuilder.substring(0, nameBuilder.length() - 1);
			}
		}

		byte[] fileBuffer = new byte[CHUNK_SIZE];
		try {
			if (encoder != null && oggEncoder) {
				while ((n = in.read(fileBuffer, 0, CHUNK_SIZE)) > 0) {
					context.getClientOut().write(fileBuffer, 0, n);
				}
			} else {
				int lastWrote = 0;
				while ((n = in.read(fileBuffer, 0, lastWrote < CHUNK_SIZE ? CHUNK_SIZE - lastWrote : CHUNK_SIZE)) > 0) {
					context.getClientOut().write(fileBuffer, 0, n);
					lastWrote += n;
					while (lastWrote >= CHUNK_SIZE) {
						lastWrote -= CHUNK_SIZE;
					}
					if (lastWrote == 0) {
						if (sendHeader) {
							context.getClientOut().write(0);
						} else {
							sendHeader = true;
							sendIcyStreamTitle(name, CHUNK_SIZE);
						}
					} else {
					}
				}
				Arrays.fill(fileBuffer, (byte) 0);
				context.getClientOut().write(fileBuffer, 0, CHUNK_SIZE - lastWrote);
				context.getClientOut().write(0);
			}
		} finally {
			if (encodedStream != null) {
				try {
					encodedStream.close();
				} catch (Exception e) {
				}
			}
			if (encoder != null) {
				encoder.kill();
			}
		}
	}
}
