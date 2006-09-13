package fr.cs.aerospace.orekit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileFilter;
import java.util.StringTokenizer;

/**
 * This class is a utility class to help finding a file in a directory tree.

 * <p>This class provides only the {@link #find find} static method to
 * help finding a file in a directory tree, when the deepest structure
 * of the directory tree (down to the file itself) is already known,
 * but the location of the subtree in the global file system is not
 * known. This occurs mainly in installation or development directory
 * trees that can be placed almost anywhere.</p>

 * @author Luc Maisonobe
 */
public class FindFile {

  /** private constructor.
   * Since this class is a utility class, no instance can be built, so
   * the constructor is private and does nothing.
   */
  private FindFile() {
  }

  /** Find a file in a directory tree.

   * <p>The search is done relatively to the current directory (value
   * of the system property <code>user.dir</code>), which can already
   * be inside the given file name. As an example, if the current
   * directory is
   * <code>/home/luc/sources/java/apex/src/fr/cs/aerospace/</code> and
   * filePath is
   * <code>src/fr/cs/aerospace/papeete/parsing/LexicalAnalyzerTestFile</code>,
   * the algorithm will first try its search by matching the
   * common <code>src/fr/cs/aerospace</code> part before recursively
   * trying to find the complete filename somewhere below the current
   * directory.</p>

   * @param filePath last part of the path corresponding to the
   * searched file
   * @param separator file separator used in filePath (can be
   * different from the system property <code>file.separator</code> if
   * the names are hard-coded in the application).
   * @return the file found
   * @exception FileNotFoundException if the file cannot be found
   */
  public static File find(String filePath, String separator)
    throws FileNotFoundException {

    // split the current directory name into parts
    File userDir = new File(System.getProperty("user.dir"));
    StringTokenizer tokenizer =
      new StringTokenizer(userDir.getPath(),
                          System.getProperty("file.separator"), false);
    String[] dParts = new String[tokenizer.countTokens()];
    for (int i = 0; i < dParts.length; ++i) {
      dParts[i] = tokenizer.nextToken();
    }

    // split the filename into parts
    tokenizer = new StringTokenizer(filePath, separator, false);
    String[] fParts = new String[tokenizer.countTokens()];
    for (int i = 0; i < fParts.length; ++i) {
      fParts[i] = tokenizer.nextToken();
    }

    // first try: are we already somewhere in the directory tree ?
    for (int iF = 0; iF < (fParts.length - 1); ++iF) {
      for (int iD = 0; iD < dParts.length; ++iD) {
        if (fParts[iF].equals(dParts[iD])) {
          File f = userDir;
          for (int i = dParts.length; i > iD; --i) {
            f = f.getParentFile();
          }
          for (int i = iF; i < fParts.length; ++i) {
            f = new File(f, fParts[i]);
          }
          if (f.exists()) {
            return f;
          }
        }
      }
    }

    // second try: we should be entirely below the current directory
    File f = recursiveFind(userDir, fParts);
    if ((f == null) || ! f.exists()) {
      throw new FileNotFoundException(filePath);
    }

    return f;

  }

  private static File recursiveFind(File base, String[] parts) {

    // try in base directory
    File f = new File(base, parts[0]);
    for (int i = 1; i < parts.length; ++i) {
      f = new File(f, parts[i]);
    }
    if (f.exists()) {
      return f;
    }

    // try in sub-directories
    File[] subDirectories = base.listFiles(new FileFilter() {
        public boolean accept(File f) {
          return f.isDirectory();
        }
      });
    for (int i = 0; i < subDirectories.length; ++i) {
      f = recursiveFind(subDirectories[i], parts);
      if (f != null) {
        return f;
      }
    }

    return null;

  }

}
