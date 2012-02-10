package pl.devsite.bigbitbox.system;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import pl.devsite.bitbox.sendables.SendableFileWithMimeResolver;
import pl.devsite.bitbox.server.BitBoxConfiguration;
import pl.devsite.bitbox.server.HttpTools;

/**
 *
 * @author dmn
 */
public class MusicEncoder {

	private static final Logger logger = Logger.getLogger(MusicEncoder.class.getName());
	private BitBoxConfiguration bitBoxConfiguration = BitBoxConfiguration.getInstance();
	private SystemProcess ffmpeg, encode;

	public MusicEncoder() {
	}

	public void kill() {
		if (ffmpeg != null) {
			try {
				ffmpeg.kill();
			} catch (Exception e) {
				logger.severe("exception killing ffmpeg: " + e.getMessage());
			}
		}
		if (encode != null) {
			try {
				encode.kill();
			} catch (Exception e) {
				logger.severe("exception killing encoder: " + e.getMessage());
			}
		}
	}

	public InputStream encode(String fileName) throws IOException {
		return encode(fileName, null);
	}

	public InputStream encode(String fileName, OutputStream errorStream) throws IOException {
		return encode(fileName, null, errorStream);
	}

	public InputStream encode(String fileName, String metaInf, OutputStream errorStream) throws IOException {
		String encoder = BitBoxConfiguration.getInstance().getProperty(BitBoxConfiguration.PROPERTY_TOOLS_ENCODER);
		boolean oggEncoder = "oggenc".equals(encoder);
		boolean lameEncoder = "lame".equals(encoder);
		boolean aacEncoder = false && "aac".equals(encoder); //disabled

		if (!lameEncoder && !oggEncoder && !aacEncoder) {
			return null;
		}

		String aacPath = bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_TOOLS_AAC);
		if ((!oggEncoder) && (aacPath == null || (new File(aacPath).canExecute() == false))) {
			throw new IOException("No executable tool: neroAacEnc");
		}
		String lamePath = bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_TOOLS_LAME);
		if ((!oggEncoder) && (lamePath == null || (new File(lamePath).canExecute() == false))) {
			throw new IOException("No executable tool: lame");
		}
		String oggencPath = bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_TOOLS_OGGENC);
		if (oggEncoder && (oggencPath == null || (new File(oggencPath).canExecute() == false))) {
			throw new IOException("No executable tool: oggenc");
		}
		String ffmpegPath = bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_TOOLS_FFMPEG);
		if (ffmpegPath == null || (new File(ffmpegPath).canExecute() == false)) {
			throw new IOException("No executable tool: ffmpeg");
		}

		String propertyAacOptions = bitBoxConfiguration == null ? null : bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_TOOLS_AAC_OPTIONS);
		final String[] aacOptions;
		if (propertyAacOptions != null) {
			String array[] = propertyAacOptions.split(" ");
			aacOptions = new String[array.length + 2];
			System.arraycopy(array, 0, aacOptions, 2, array.length);
			aacOptions[0] = "-";
			aacOptions[1] = "-";
		} else {
			aacOptions = "- - 48".split(" ");
		}

		String propertyLameOptions = bitBoxConfiguration == null ? null : bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_TOOLS_LAME_OPTIONS);
		final String[] lameOptions;
		if (propertyLameOptions != null) {
			String array[] = propertyLameOptions.split(" ");
			lameOptions = new String[array.length + 2];
			System.arraycopy(array, 0, lameOptions, 0, array.length);
			lameOptions[lameOptions.length - 2] = "-";
			lameOptions[lameOptions.length - 1] = "-";
		} else {
			//lameOptions = "--preset radio - -".split(" ");
			//lameOptions = "--preset cbr 96 - -".split(" ");
			lameOptions = "-m j -q 7 --resample 44.1 -b 96 - -".split(" ");
		}

		String propertyOggencOptions = bitBoxConfiguration == null ? null : bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_TOOLS_OGGENC_OPTIONS);
		final String[] oggencOptions;
		ArrayList<String> tag = null;
		if (oggEncoder && metaInf != null && !metaInf.isEmpty()) {
			ArrayList<String> result = new ArrayList<String>();
			Map<String, String> tagMap = HttpTools.headersToMap(metaInf);
			String pre = SendableFileWithMimeResolver.AUDIO_HTTP_HEADER_PREFIX.toLowerCase();
			/*
			 * String album = tagMap.get(pre + "album"); String title = tagMap.get(pre + "title"); String artist =
			 * tagMap.get(pre + "artist"); String tracknum = tagMap.get(pre + "tracknumber"); tracknum = tracknum ==
			 * null ? null : tagMap.get(pre + "track"); tracknum = tracknum == null ? null : tagMap.get(pre +
			 * "tracknum"); String date = tagMap.get(pre + "date"); String genre = tagMap.get(pre + "genre"); //String
			 * title = "a";String album="b"; String artist="c";String tracknum="99";String date="2000";String
			 * genre="something cool"; if (album != null) { result.add("--album"); result.add(album); } if (title !=
			 * null) { result.add("--title"); result.add(title); } if (artist != null) { result.add("--artist");
			 * result.add(artist); } if (tracknum != null) { result.add("--tracknum"); result.add(tracknum); } if (date
			 * != null) { result.add("--date"); result.add(date); } if (genre != null) { result.add("--genre");
			 * result.add(genre);
			}
			 */
			for (Entry<String, String> entry : tagMap.entrySet()) {
				if (entry.getKey().startsWith(pre)) {
					result.add("-c");
					result.add(entry.getKey().substring(pre.length())
							+ "=" + entry.getValue());
				}
			}
			tag = result;
		}
		if (propertyOggencOptions == null) {
			propertyOggencOptions = "-q 0";
		}
		{
			String array[] = (propertyOggencOptions).split(" ");
			oggencOptions = new String[array.length + 1 + (tag == null ? 0 : tag.size())];
			System.arraycopy(array, 0, oggencOptions, 0, array.length);
			oggencOptions[oggencOptions.length - 1] = "-";
			if (tag != null) {
				for (int i = 0; i < tag.size(); i++) {
					oggencOptions[array.length + i] = tag.get(i);
				}
			}
		}

		ffmpeg = new SystemProcess(ffmpegPath, new String[]{
					"-i", fileName, "-f", "wav", "-"
				});
		//ffmpeg.getProcessIn().write(("q/n".getBytes()));

		if (lameEncoder) {
			encode = new SystemProcess(lamePath, lameOptions);
		} else if (oggEncoder) {
			encode = new SystemProcess(oggencPath, oggencOptions);
		} else if (aacEncoder) {
			encode = new SystemProcess(aacPath, aacOptions);
		}

		if (errorStream != null) {
			SystemProcess.pumpBackground(ffmpeg.getProcessErr(), errorStream);
			SystemProcess.pumpBackground(encode.getProcessErr(), errorStream);
		}

		logger.info("executing " + ffmpeg + " | " + encode);

		return ffmpeg.pipeBackground(encode).getProcessStd();
	}

	private static class ConsoleOutputStream extends OutputStream {

		String name;
		int last;

		public ConsoleOutputStream(String name) {
			this.name = name;
		}

		@Override
		public void write(int i) throws IOException {
			if (name != null && (last == 13 || last == 10)) {
				System.out.print(name + ": ");
			}
			System.out.print((char) (i));
			last = i;
		}

		public static void main(String[] args) throws IOException, InterruptedException, Exception {
			try {
				File outputFile = new File("/tmp/a.java");
				FileOutputStream file = new FileOutputStream(outputFile);

				OutputStream errorStream = new ConsoleOutputStream(null);

				MusicEncoder encoder = new MusicEncoder();
				//encoder.encode("/home/dmn/mp3/a.mp3");
				InputStream a = encoder.encode("/home/dmn/mp3/a.ogg", errorStream);
				SystemProcess.pump(a, file);
			} catch (Exception e) {
				throw e;
			} finally {
				Thread.sleep(500);
				System.exit(0);
			}
		}
	}
}
