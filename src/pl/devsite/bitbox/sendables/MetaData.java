package pl.devsite.bitbox.sendables;

import java.util.HashMap;
import pl.devsite.bitbox.server.HttpTools;

/**
 *
 * @author dmn
 */
public interface MetaData {

	String getMetadataValue(String key);

	String getMetadata();

	public class Provider {

		private String metadata;
		private HashMap<String, String> metadataMap;

		public Provider(String value) {
			this.metadata = value;
			if (metadata != null && !metadata.isEmpty()) {
				metadataMap = HttpTools.headersToMap(metadata);
			}
		}

		public String getMetadata() {
			return metadata;
		}

		public String get(String key) {
			if (key == null || metadata == null || metadata.isEmpty()) {
				return null;
			}
			return metadataMap.get(key);
		}
	}
}
