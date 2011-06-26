package pl.devsite.bitbox.server;

import java.util.ArrayList;

/**
 *
 * @author dmn
 */
public class HttpHeader {

    private ArrayList<String> data;

    public HttpHeader(String request) {
        this();
        if (request.length() > 0) {
            for (String line : request.split("\r\n")) {
                int colonPos = line.indexOf(HttpTools.COLON);
                if (colonPos > 0) {
                } else {
                }
                data.add(line);
            }
        }
    }

    public HttpHeader() {
        data = new ArrayList<String>();
    }

    public String get(String key) {
        String keyLower = key.toLowerCase();
        for (String line : data) {
            if (line.toLowerCase().startsWith(keyLower + HttpTools.COLON)) {
                return line.substring(key.length()+1).trim();
            }
        }
        return null;
    }

    public void add(String key, String value) {
        data.add(key + ": " + value);
    }

    @Override
    public String toString() {
        if (data == null || data.size() == 0) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        for (String line : data) {
            result.append(line+"\r\n");
        }
        return result.toString();
    }


}
