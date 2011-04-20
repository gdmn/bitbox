package pl.devsite.configuration;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Damian Gorlach
 */
public interface ConfigurationListener {

    void readConfiguration(File file) throws IOException;

    void readConfiguration(List<String> args);
}
