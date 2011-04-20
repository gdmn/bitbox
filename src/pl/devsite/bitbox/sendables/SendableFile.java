package pl.devsite.bitbox.sendables;

import pl.devsite.bitbox.server.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SendableFile extends SendableAdapter {

    protected File file;

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
        name = name.replace("/", "").replace("\\", "").replace(":", "");
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
            Logger.getLogger(SendableFile.class.getName()).log(Level.SEVERE, null, ex);
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

    @Override
    public boolean isRawFile() {
        return !file.isDirectory();
    }

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
        String[] listFiles;
        listFiles = file.list();
        String thisLocalStr = getLocalPath();
        String lower = id.toString().toLowerCase();
        for (int i = 0; i < listFiles.length; i++) {
            if (listFiles[i].toLowerCase().equals(lower)) {
                String fi = listFiles[i];
                return getInstance(this, new File(thisLocalStr + fi));
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
                Logger.getLogger(SendableFile.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (file.isDirectory() && !result.endsWith(java.io.File.separator)) {
                result = result + java.io.File.separator;
            }
            return result;
        }
        return null;
    }
}
