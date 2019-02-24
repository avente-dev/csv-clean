package tech.avente;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tika.detect.AutoDetectReader;
import org.apache.tika.exception.TikaException;

/**
 * CSV cleaning main class.
 * 
 * @author Jeremy Moore
 */
public class CsvClean {

	private static final String EOL = "\r\n";
	private static final String ENCODING = "windows-1252";
	private static final String REPLACEMENT = ">";

	private static final Logger logger = Logger.getLogger(CsvClean.class.getName());

	/**
	 * Clean a CSV file with windows-1252 encoding.
	 *
	 * @param args The CSV file path must be the first command line argument.
	 */
	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			logger.log(Level.SEVERE, "File argument required.");
			System.exit(1);
		}
		final File csvFile = new File(args[0]);
		if (!csvFile.isFile()) {
			logger.log(Level.SEVERE, "File not found: " + csvFile.getPath());
			System.exit(1);
		}
		if (!csvFile.getName().endsWith(".csv")) {
			logger.log(Level.SEVERE, "File does not have \".csv\" extension: " +  csvFile.getPath());
			System.exit(1);
		}
		final Charset charset = detectCharset(csvFile);
		if (charset == null) {
			logger.log(Level.SEVERE, "File encoding could not be detected.");
			System.exit(1);
		}
		if (!ENCODING.equals(charset.toString())) {
			logger.log(Level.SEVERE, "File encoding: " + charset.toString() + ". Must be: " + ENCODING);
			System.exit(1);
		}

		File tmpFile = cleanFile(csvFile, charset);
		if (tmpFile == null) {
			logger.log(Level.SEVERE, "Failed to clean file.");
			System.exit(1);
		}

		if (!moveTo(tmpFile, csvFile)) {
			logger.log(Level.SEVERE, "Failed to replace file.");
			System.exit(1);
		}
	}

	private static Charset detectCharset(File file) {
		try (FileInputStream fis = new FileInputStream(file); AutoDetectReader detector = new AutoDetectReader(fis)) {
			return detector.getCharset();
		} catch (IOException | TikaException e) {
			logger.log(Level.SEVERE, null, e);
			return null;
		}
	}

	private static String timestamp() {
		return Long.toString(new Date().getTime());
	}

	private static File tmpFile(File file) {
		return new File(file.getPath() + "." + timestamp() + ".tmp");
	}

	private static boolean moveTo(File srcFile, File dstFile) {
		return srcFile.renameTo(dstFile);
	}

	private static File cleanFile(File csvFile, Charset charset) {

		File tmpFile = tmpFile(csvFile);

		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tmpFile), charset)) {

			List<String> lines = Files.readAllLines(csvFile.toPath(), charset);
			for (String line : lines) {

				String clean = replaceNonPrintable(line);
				writer.write(clean + EOL);

				if (!clean.equals(line)) {
					System.out.println("---");
					System.out.println("< " + line);
					System.out.println("> " + clean);
				}

			}

			return tmpFile;

		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
			return null;
		}
	}

	private static String replaceNonPrintable(String s) {
		return s.replaceAll("\\P{Print}", REPLACEMENT);
	}

}
