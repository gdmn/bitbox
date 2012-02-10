package pl.devsite.bitbox.sendables;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;

/**
 *
 * @author dmn
 */
public class MimeResolver {

	private static MimeResolver instance;
	private HashMap<String, String> mimeMap = new HashMap<String, String>();
	private boolean mimeMapInitialized = false;

	private MimeResolver() {
	}

	public static MimeResolver getInstance() {
		if (instance != null) {
			return instance;
		}
		instance = new MimeResolver();
		return instance;
	}

	private HashMap<String, String> getMimeMapInstance() {
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

	public String resolveFileExt(String fileExt) {
		final String defaultType = "application/octet-stream";
		HashMap<String, String> map = getMimeMapInstance();
		String result = map.get(fileExt);
		if (result == null) {
			result = defaultType;
		}
		return result;
	}
}
