package fr.cs.orekit.iers;

import java.io.File;
import java.net.URL;

import fr.cs.orekit.errors.OrekitException;

/** Helper class for loading IERS data files.

 * <p>
 * This class handles the IERS data files recursively starting
 * from a root root tree specified by the java property
 * <code>orekit.iers.directory</code>. If the property is not set or is null,
 * no IERS data will be used (i.e. no pole correction and no UTC steps will
 * be taken into account) and no errors will be triggered.
 * If the property is set, it must correspond to an
 * existing root tree otherwise an error will be triggered. The organisation
 * of files in the tree is free, sub-directories can be used at will.</p>
 * <p>Gzip-compressed files are supported.</p>
 * 
 * <p>
 * This is a simple application of the <code>visitor</code> design pattern for
 * directory hierarchy crawling.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public class IERSDirectoryCrawler {

  /** Private constructor for the singleton.
   * @exception OrekitException if some data is missing or can't be read
   */
  public IERSDirectoryCrawler()
    throws OrekitException {

    // check the root tree
    String directoryName = System.getProperty("orekit.iers.directory");
    if ((directoryName != null) && ! "".equals(directoryName)) {

      // try to find the root directory either in classpath or in filesystem
      // (classpath having higher priority)
      URL url = getClass().getClassLoader().getResource(directoryName);
      root = new File((url != null) ? url.getPath() : directoryName);

      // safety checks
      if (! root.exists()) {
        throw new OrekitException("IERS root directory {0} does not exist",
                                  new String[] { root.getAbsolutePath() });
      }
      if (! root.isDirectory()) {
        throw new OrekitException("{0} is not a directory",
                                  new String[] { root.getAbsolutePath() });
      }

    }

  }

  /** Crawl the IERS root hierarchy.
   * @param visitor IERS file visitor to use
   * @exception OrekitException if some data is missing, duplicated
   * or can't be read
   */
  public void crawl(IERSFileVisitor visitor)
    throws OrekitException {
    if (root != null) {
      crawl(visitor, root);
    }
  }

  /** Crawl a directory hierarchy.
   * @param visitor IERS file visitor to use
   * @param directory hierarchy root directory
   * @exception OrekitException if some data is missing, duplicated
   * or can't be read
   */
  private void crawl(IERSFileVisitor visitor, File directory)
    throws OrekitException {

    // search in current directory
    File[] list = directory.listFiles();
    for (int i = 0; i < list.length; ++i) {
      if (list[i].isDirectory()) {

        // recurse in the sub-directory
        crawl(visitor, list[i]);

      } else  if (visitor.fileIsSupported(list[i])) {

        // visit the current file
        visitor.visit(list[i]);

      }
    }

  }

  /** IERS root hierarchy root. */
  private File root;

}
