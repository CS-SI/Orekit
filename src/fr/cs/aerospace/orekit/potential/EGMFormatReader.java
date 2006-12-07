package fr.cs.aerospace.orekit.potential;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import fr.cs.aerospace.orekit.errors.OrekitException;

/**This reader is adapted to the EGM Format.
 *  
 * <p> The proper way to use this class is to call the 
 *  {@link fr.cs.aerospace.orekit.potential.PotentialReaderFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file <p>
 *   
 * @see fr.cs.aerospace.orekit.potential.PotentialReaderFactory
 * @author F. Maussion
 */
public class EGMFormatReader extends PotentialCoefficientsReader {

  /** Simple constructor (the first method to call after construction is 
   * {@link #isFileOK(InputStream)}. It is done automaticaly by the factory).
   */
  protected EGMFormatReader() {
    in = null;
    fileIsOK = false;
    ae = 6378136.3;
    mu = 398600.4415e8;
  }

  /** Check the file to determine if its format is understood by the reader or not.
   * @param in the input to check
   * @return true if it is readable, false if not.
   * @throws IOException when the {@link InputStream} cannot be buffered.
   */
  public boolean isFileOK(InputStream in) throws IOException {

    this.in = in;
    BufferedReader r = new BufferedReader(new InputStreamReader(in));

    // tests variables
    boolean iKnow = false;
    int lineIndex = 0;
    int c = 1;

    // set up the regular expressions
    String integerField = " +[0-9]+";
    String realField = " +[-+0-9.e.E]+";
    Pattern regularPattern = Pattern.compile("^" + integerField + integerField
        + realField + realField + realField + realField + " *$");

    // read the first lines to detect the format
    for (String line = r.readLine(); !iKnow; line = r.readLine()) {
      if (line == null) {
        iKnow = true;
      } else {
        Matcher matcher = regularPattern.matcher(line);
        if (matcher.matches()) {
          lineIndex++;
        }
        if ((lineIndex == c) && (c > 2)) {
          iKnow = true;
          fileIsOK = true;
        }
        if ((lineIndex != c) && (c > 2)) {
          iKnow = true;
          fileIsOK = false;
        }
        c++;
      }
    }
    return fileIsOK;
  }

  /** Computes the coefficients by reading the selected (and tested) file 
   * @throws OrekitException when the file has not been initialized or checked.
   * @throws IOException when the file is corrupted.
   */
  public void read() throws OrekitException, IOException {

    if (in == null) {
      throw new OrekitException("the reader has not been tested ",
                                new String[0]);
    }
    if (!fileIsOK) {
      throw new OrekitException("the reader is not adapted to the format ",
                                new String[0]);
    }
    BufferedReader r = new BufferedReader(new InputStreamReader(in));

    ArrayList cl = new ArrayList();
    ArrayList sl = new ArrayList();
    for (String line = r.readLine(); line != null; line = r.readLine()) {
      if (line.length() >= 15) {

        // get the fields defining the current the potential terms
        String[] tab = line.trim().split("\\s+");
        int i = Integer.parseInt(tab[0]);
        int j = Integer.parseInt(tab[1]);
        double c = Double.parseDouble(tab[2]);
        double s = Double.parseDouble(tab[3]);

        // extend the cl array if needed
        while (cl.size() <= i) {
          double[] row = new double[cl.size() + 1];
          cl.add(row);
        }

        // extend the sl array if needed
        while (sl.size() <= i) {
          double[] row = new double[sl.size() + 1];
          sl.add(row);
        }

        // store the terms
        ((double[]) cl.get(i))[j] = c;
        ((double[]) sl.get(i))[j] = s;

      }
    }

    // convert to simple triangular arrays
    normalizedC = (double[][]) cl.toArray(new double[cl.size()][]);
    normalizedS = (double[][]) sl.toArray(new double[sl.size()][]);

  }

  /** is file ok? */
  private boolean fileIsOK;

  /** The input to check and read */
  private InputStream in;

}
