package fr.cs.aerospace.orekit.frames.nutation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cs.aerospace.orekit.errors.OrekitException;

/**
 * Class representing a development of nutation computations.
 * <p>
 * Nutation developments are time polynomials. The coefficients of the
 * polynomials are themselves summation series. The
 * {@link NutationSerieTerm series terms} are harmonic functions (combination of
 * sines and cosines) of general <em>arguments</em>. The arguments are
 * combination of luni-solar or planetary {@link BodiesElements elements}.
 * </p>
 * 
 * @author Luc Maisonobe
 * @see NutationSerieTerm
 */
public class NutationDevelopment {

  /** Build a development from an IERS table file.
   * @param stream stream containing the IERS table
   * @param name name of the resource file (for error messages only)
   * @exception OrekitException if the table cannot be parsed
    */
  public NutationDevelopment(InputStream stream, String name)
    throws OrekitException {

    try {
      // the polynomial part should read something like:
      // -16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5
      String signField  = "\\p{Space}*\\([-+]\\)";
      String coeffField = "\\p{Space}*\\(\\p{Digit}+\\.\\p{Digit}+\\)";
      String tField     = "\\p{Space}*t\\^\\p{Digit}";
      Pattern polynomialPattern =
        Pattern.compile("^" + signField + coeffField + signField + coeffField + "t"
                        + signField + coeffField + tField + signField + coeffField + tField
                        + signField + coeffField + tField + signField + coeffField + tField
                        + "\\p{Space}*$");

      // the series parts should read something like:
      // j = 0  Nb of terms = 1306        
      // 
      //  1    -6844318.44        1328.67    0    0    0    0    1    0    0    0    0    0    0    0    0    0
      //  2     -523908.04        -544.76    0    0    2   -2    2    0    0    0    0    0    0    0    0    0
      //  3      -90552.22         111.23    0    0    2    0    2    0    0    0    0    0    0    0    0    0
      //  4       82168.76         -27.64    0    0    0    0    2    0    0    0    0    0    0    0    0    0
      Pattern serieHeaderPattern =
        Pattern.compile("^\\p{Space}*j\\p{Space}*=\\p{Space}*\\(\\p{Digit}\\)"
                        + ".*"
                        + "=\\p{Space}*\\(\\p{Digit}+\\)\\p{Space}*$");


      // setup the reader
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      int lineNumber = 0;

      // look for the polynomial part
      for (String line = reader.readLine(); (line != null) && (coefficients == null); line = reader.readLine()) {
        ++lineNumber;
        Matcher matcher = polynomialPattern.matcher(line);
        if (matcher.matches()) {
          coefficients = new double[5];
          for (int i = 0; i < coefficients.length; ++i) {
            coefficients[i] = Double.parseDouble(matcher.group(2 * (i + 1)));
            if ("-".equals(matcher.group(2 * i + 1))) {
              coefficients[i] = - coefficients[i];
            }
          }
        }
      }
      if (coefficients == null) {
        throw new OrekitException("file {0} is not an IERS data file",
                                  new String[] { name });
      }

      // look for the non-polynomial part
      ArrayList array = new ArrayList();
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        ++lineNumber;
        Matcher matcher = serieHeaderPattern.matcher(line);
        if (matcher.matches()) {

          // sanity check
          if (Integer.parseInt(matcher.group(1)) != array.size()) {
            throw new OrekitException("missing serie j = {0} in nutation model file {1} (line {2})",
                                      new String[] {
                                        Integer.toString(array.size()),
                                        name,
                                        Integer.toString(lineNumber)
                                      });
          }

          int nTerms = Integer.parseInt(matcher.group(2));
          NutationSerieTerm[] serie = new NutationSerieTerm[nTerms];

          // skip blank lines
          line = reader.readLine();
          ++lineNumber;
          while ((line != null) && "".equals(line.trim())) {
            line = reader.readLine();
            ++lineNumber;
          }

          // read the terms of the current serie
          for (int i = 0; i < nTerms; ++i) {

            // sanity check
            if (line == null) {
              throw new OrekitException("unexpected end of nutation model file {1} (after line {2})",
                                        new String[] {
                                          name,
                                          Integer.toString(lineNumber - 1)
                                        });
            }

            // parse the nutation serie term
            String[] fields = line.split("\\p{Space}");
            if (fields.length != 17) {
              throw new OrekitException("unable to parse line {1} of nutation model file {2}",
                                        new String[] {
                                          Integer.toString(lineNumber), name
                                        });              
            }
            serie[i] =
              NutationSerieTerm.buildTerm(Double.parseDouble(fields[1]) * radiansPerMicroAS,
                                          Double.parseDouble(fields[2]) * radiansPerMicroAS,
                                          Integer.parseInt(fields[ 3]), Integer.parseInt(fields[ 4]),
                                          Integer.parseInt(fields[ 5]), Integer.parseInt(fields[ 6]),
                                          Integer.parseInt(fields[ 7]), Integer.parseInt(fields[ 8]),
                                          Integer.parseInt(fields[ 9]), Integer.parseInt(fields[10]),
                                          Integer.parseInt(fields[11]), Integer.parseInt(fields[12]),
                                          Integer.parseInt(fields[13]), Integer.parseInt(fields[14]),
                                          Integer.parseInt(fields[15]), Integer.parseInt(fields[16]));

            // go to next line
            line = reader.readLine();
            ++lineNumber;

          }

          // the serie has been completed, store it
          array.add(serie);

        }
      }
      if (array.isEmpty()) {
        throw new OrekitException("file {0} is not an IERS data file",
                                  new String[] { name });        
      }

      // store the non-polynomial part series
      terms = (NutationSerieTerm[][]) array.toArray(new NutationSerieTerm[array.size()][]);

    } catch (IOException ioe) {
      throw new OrekitException(ioe.getMessage(), ioe);
    }

  }

  /** Compute the value of the development for the current date.
   * @param t current date
   * @param elements luni-solar and planetary elements for the current date
   * @return current value of the development
   */
  public double value(double t, BodiesElements elements) {

    // polynomial part
    double p = 0;
    for (int i = coefficients.length - 1; i >= 0; --i) {
      p = p * t + coefficients[i];
    }

    // non-polynomial part
    double np = 0;
    for (int i = terms.length - 1; i >= 0; --i) {

      NutationSerieTerm[] serie = terms[i];

      // add the harmonic terms starting from the last (smallest) terms,
      // to avoid numerical problems
      double s = 0;
      for (int k = serie.length - 1; k >= 0; --k) {
        s += serie[k].value(elements);
      }

      np = np * t + s;

    }

    // add the polynomial and the non-polynomial parts
    return p + np;

  }

  /** Coefficients of the polynomial part. */
  private double[] coefficients;

  /** Series terms. */
  private NutationSerieTerm[][] terms;

  /** 2&pi;. */
  private static final double twoPi = 2.0 * Math.PI;

  /** Radians per microarcsecond. */
  private static final double radiansPerMicroAS = twoPi / 1.296e12;

}
