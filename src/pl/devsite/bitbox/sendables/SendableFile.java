package pl.devsite.bitbox.sendables;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.server.renderers.HtmlLister;

public class SendableFile extends SendableAdapter {

	protected File file;
	private static final Logger logger = Logger.getLogger(SendableFile.class.getName());

	public SendableFile(Sendable parent, File file) {
		// (new File("c:/")).getName() --> ""
		super(parent, null);
		try {
			String n = file.getName();
			name = (n != null && n.length() > 0) ? n : file.getCanonicalPath();
			this.file = file;
		} catch (IOException ex) {
			name = file.toString();
		}
		name = name.replace("/", "").replace("\\", "").replaceAll("[^a-zA-Z0-9 ,.\\-\\(\\)\\[\\]]", "_");
	}

	@Override
	public InputStream getResponseStream() {
		try {
			if (!file.exists()) {
				return null;
			} else if (file.isDirectory()) {
				return new HtmlLister(this);
			} else {
				return new FileInputStream(file);
			}
		} catch (FileNotFoundException ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		} finally {
		}
		return null;
	}

	@Override
	public String getMimeType() {
		if (file.isDirectory()) {
			return "text/html";
		} else {
			return "application/octet-stream";
		}
	}

	@Override
	public long getContentLength() {
		if (file.isDirectory()) {
			return -1;
		} else {
			return file.length();
		}
	}

//	@Override
//	public boolean isRawFile() {
//		return !file.isDirectory();
//	}

	@Override
	public boolean hasChildren() {
		return file.isDirectory();
	}

	@Override
	public Sendable[] getChildren() {
		if (!file.isDirectory()) {
			return null;
		}
		File[] listFiles;
		listFiles = file.listFiles();
		Sendable[] result = new Sendable[listFiles.length];
		int count = 0;
		for (int i = 0; i < listFiles.length; i++) {
			if (!listFiles[i].isHidden()) {
				result[count++] = getInstance(this, listFiles[i]);
			}
		}
		result = Arrays.copyOf(result, count);
		return result;
	}

	protected Sendable getInstance(Sendable parent, File file) {
		return new SendableFile(parent, file);
	}

	@Override
	public Sendable getChild(Object id) {
		if (!file.isDirectory()) {
			return null;
		}
		for (Sendable ch : getChildren()) {
			SendableAdapter cha = (SendableAdapter) ch;
			if (id.equals(cha.getName())) {
				return cha;
			}
		}
		return null;
	}

	@Override
	public String getAddress() {
		if (file != null) {
			return super.getAddress();
		}
		return null;
	}

	private String getLocalPath() {
		if (file != null) {
			String result = null;
			try {
				result = file.getCanonicalPath().toString();
			} catch (IOException ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
			if (file.isDirectory() && !result.endsWith(java.io.File.separator)) {
				result = result + java.io.File.separator;
			}
			return result;
		}
		return null;
	}

	public File getFile() {
		return file;
	}

}
