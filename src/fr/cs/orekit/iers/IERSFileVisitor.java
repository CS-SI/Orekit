package fr.cs.orekit.iers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import fr.cs.orekit.errors.OrekitException;


/** Base class for IERS files visitors.
 * @see IERSDirectoryCrawler#visit
 * @author Luc Maisonobe
 */
public abstract class IERSFileVisitor {

  /** Simple constructor.
   * @param supportedFilesPattern file name pattern for supported files
   */
  protected IERSFileVisitor(String supportedFilesPattern) {
    this.supportedFilesPattern = Pattern.compile(supportedFilesPattern);
  }

  /** Check if a file is supported.
   * <p>Checking is performed only on file name</p>
   * @param file file to check
   * @return true if file name correspond to a supported file
   */
  public boolean fileIsSupported(File file) {
    return supportedFilesPattern.matcher(file.getName()).matches();
  }

  /** Visit a file.
   * @param String fileName file name
   * @exception OrekitException if some data is missing, can't be read
   * or if some loader specific error occurs
   */
  public void visit(File file)
  throws OrekitException {
    try {
      this.file = file;
      InputStream is = new FileInputStream(file);
      if (file.getName().endsWith(".gz")) {
        // add the decompression filter
        is = new GZIPInputStream(is);
      }
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      visit(reader);
      reader.close();
    } catch (IOException ioe) {
      throw new OrekitException(ioe.getMessage(), ioe);
    } catch (ParseException pe) {
      throw new OrekitException(pe.getMessage(), pe);
    }
  }

  /** Visit a file from a reader.
   * @param reader data stream reader
   * @exception IOException if data can't be read
   * @exception ParseException if data can't be parsed
   * @exception OrekitException if some data is missing
   * or if some loader specific error occurs
   */
  protected abstract void visit(BufferedReader reader)
    throws IOException, ParseException, OrekitException;

  /** File name pattern. */
  private final Pattern supportedFilesPattern;

  /** Current file. */
  protected File file;

}
