package pl.devsite.bitbox.sendables;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bigbitbox.system.Soxi;
import pl.devsite.bitbox.server.HttpTools;

/**
 *
 * @author dmn
 */
public class SendableFileWithMimeResolver extends SendableFile implements MetaData {

	public static String AUDIO_HTTP_HEADER_PREFIX = "Audio-";
	private MimeResolver mimeResolver = MimeResolver.getInstance();
	private static final Logger logger = Logger.getLogger(SendableFileWithMimeResolver.class.getName());
	private MetaData.Provider metaData;

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
			return mimeResolver.resolveFileExt(fileExt);
		}
	}

	@Override
	public String getMetadataValue(String key) {
		if (metaData == null) {
			getMetadata();
		}
		return metaData.get(key);
	}

	@Override
	public String getMetadata() {
		if (metaData == null) {
			String mime = getMimeType();
			StringBuilder result = new StringBuilder();
			if (mime.startsWith("audio/")) {
				String audioInfo;
				try {
					audioInfo = Soxi.query(getFile().getCanonicalPath());
					if (audioInfo != null) {
						audioInfo = AUDIO_HTTP_HEADER_PREFIX + audioInfo.trim().replace("\n", "\n" + AUDIO_HTTP_HEADER_PREFIX);
						audioInfo = audioInfo.replace("\r", "").replace("\n", HttpTools.RN);
						result.append(audioInfo).append(HttpTools.RN);
					}
				} catch (IOException ex) {
					logger.log(Level.SEVERE, null, ex);
				}
			}
			if (result.length() > 0) {
				metaData = new MetaData.Provider(result.substring(0, result.length() - HttpTools.RN.length()));
			} else {
				metaData = new MetaData.Provider(null);
			}
		}
		return metaData.getMetadata();
	}

	@Override
	protected Sendable getInstance(Sendable parent, File file) {
		return new SendableFileWithMimeResolver(parent, file);
	}
}
