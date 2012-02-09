package pl.devsite.bitbox.sendables;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bigbitbox.system.Soxi;
import pl.devsite.bitbox.server.HttpTools;

/**
 *
 * @author dmn
 */
public class SendableFileWithMimeResolver extends SendableFile {

	public static String AUDIO_HTTP_HEADER_PREFIX = "Audio-";
	private String metadata;
	private boolean metadataCollected = false;
	private HashMap<String, String> metadataMap;
	private static HashMap<String, String> mimeMap = new HashMap<String, String>();
	private static boolean mimeMapInitialized = false;

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
            return resolveFileExt(fileExt);
        }
    }

	public String getMetadataValue(String key) {
		if (!metadataCollected) {
			getMetadata();
		}
		if (metadata == null) {
			return null;
		}
		if (metadataMap == null) {
			metadataMap = HttpTools.headersToMap(metadata);
		}
		return metadataMap.get(key.toLowerCase());
	}

	public String getMetadata() {
		if (metadataCollected) {
			return metadata;
		}
		String mime = getMimeType();
		StringBuilder result = new StringBuilder();
		if (mime.startsWith("audio/")) {
			String audioInfo = null;
			try {
				audioInfo = Soxi.query(getFile().getCanonicalPath());
				if (audioInfo != null) {
					audioInfo = AUDIO_HTTP_HEADER_PREFIX + audioInfo.trim().replace("\n", "\n" + AUDIO_HTTP_HEADER_PREFIX);
					audioInfo = audioInfo.replace("\r", "").replace("\n", HttpTools.BR);
					result.append(audioInfo).append(HttpTools.BR);
				}
			} catch (IOException ex) {
				Logger.getLogger(HttpTools.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		metadataCollected = true;
		if (result.length() > 0) {
			metadata = result.substring(0, result.length() - HttpTools.BR.length());
		} else {
			metadata = null;
		}
		return metadata;
	}

    @Override
    protected Sendable getInstance(Sendable parent, File file) {
        return new SendableFileWithMimeResolver(parent, file);
    }
	
	private static HashMap<String, String> getMimeMapInstance() {
		if (mimeMapInitialized) {
			return mimeMap;
		}
		InputStream mimeResourceStream = SendableFileWithMimeResolver.class.getResourceAsStream("resources/mime.types");
		Scanner scanner = new Scanner(mimeResourceStream, "UTF-8");
		try {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim().toLowerCase();
				if (line.isEmpty()) {
					continue;
				}
				if (line.startsWith("#")) {
					continue;
				}
				String backup;
				do {
					backup = line;
					line = line.replace("\t", " ");
					line = line.replace("  ", " ");
				} while (!line.equals(backup));
				String lineSep[] = line.split(" ");
				for (int i = 1; i < lineSep.length; i++) {
					mimeMap.put(lineSep[i], lineSep[0]);
					//System.out.println("\t" + lineSep[i] + "=" + lineSep[0]);
				}
			}
			return mimeMap;
		} finally {
			mimeMapInitialized = true;
			scanner.close();
		}
	}

	public static String resolveFileExt(String fileExt) {
		final String defaultType = "application/octet-stream";
		HashMap<String, String> map = getMimeMapInstance();
			String result = map.get(fileExt);
			if (result == null) {
				result = defaultType;
			}
			return result;
    }
}
