package pl.devsite.bigbitbox.system;

import java.io.*;
import java.util.logging.Logger;
import pl.devsite.bitbox.server.BitBoxConfiguration;

/**
 *
 * @author dmn
 */
public class MusicEncoder {

	private static final Logger logger = Logger.getLogger(MusicEncoder.class.getName());
	private BitBoxConfiguration bitBoxConfiguration = BitBoxConfiguration.getInstance();
	private SystemProcess ffmpeg, lame;

	public MusicEncoder() {
	}

	public void kill() {
		if (ffmpeg != null) {
			try {
				ffmpeg.kill();
			} catch (Exception e) {
				logger.severe("exception killing ffmpeg: "+e.getMessage());
			}
		}
		if (lame != null) {
			try {
				lame.kill();
			} catch (Exception e) {
				logger.severe("exception killing lame: "+e.getMessage());
			}
		}
	}

	public InputStream encode(String fileName) throws IOException {
		return encode(fileName, null);
	}

	public InputStream encode(String fileName, OutputStream errorStream) throws IOException {
		String lamePath = bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_LAME);
		if (lamePath == null || (new File(lamePath).canExecute() == false)) {
			throw new IOException("No executable tool: lame");
		}
		String ffmpegPath = bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_FFMPEG);
		if (ffmpegPath == null || (new File(ffmpegPath).canExecute() == false)) {
			throw new IOException("No executable tool: ffmpeg");
		}
		String propertyLameOptions = bitBoxConfiguration == null ? null : bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_LAME_OPTIONS);
		final String[] lameOptions;
		if (propertyLameOptions != null) {
			String propertyLameOptionsArray[] = propertyLameOptions.split(" ");
			lameOptions = new String[propertyLameOptionsArray.length + 2];
			System.arraycopy(propertyLameOptionsArray, 0, lameOptions, 0, propertyLameOptionsArray.length);
			lameOptions[lameOptions.length - 2] = "-";
			lameOptions[lameOptions.length - 1] = "-";
		} else {
			//lameOptions = "--preset radio - -".split(" ");
			//lameOptions = "--preset cbr 96 - -".split(" ");
			lameOptions = "-m j -q 7 --resample 44.1 -b 96 - -".split(" ");
		}
		ffmpeg = new SystemProcess(ffmpegPath, new String[]{
					"-i", fileName, "-f", "wav", "-"
				});
		//ffmpeg.getProcessIn().write(("q/n".getBytes()));

		lame = new SystemProcess(lamePath, lameOptions);

		if (errorStream != null) {
			SystemProcess.pumpBackground(ffmpeg.getProcessErr(), errorStream);
			SystemProcess.pumpBackground(lame.getProcessErr(), errorStream);
		}

		logger.info("executing " + ffmpeg + " | " + lame);

		return ffmpeg.pipeBackground(lame).getProcessStd();
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
				File outputFile = new File("/tmp/a.java.mp3");
				FileOutputStream file = new FileOutputStream(outputFile);

				OutputStream errorStream = new ConsoleOutputStream(null);

				MusicEncoder encoder = new MusicEncoder();
				//encoder.encode("/home/dmn/mp3/yeah.ogg");
				InputStream a = encoder.encode("/home/dmn/mp3/yeah.ogg", errorStream);
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
