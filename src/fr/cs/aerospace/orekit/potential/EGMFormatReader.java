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
public class EGMFormatReader implements PotentialCoefficientsReader {
  
  /** Simple constructor (the first method to call then is <code>isFileOK</code>).
   */
  protected EGMFormatReader() {
    fileIsOK = false;
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
    // read the first lines to detect the format
    for (String line = r.readLine(); iKnow!=true; line=r.readLine()) {  
      if (line==null) {
        iKnow = true;
      }
      else {
        String integerField = " +[0-9]+";
        String realField = " +[-+0-9.e.E]+";
        
        Pattern regularPattern =
          Pattern.compile("^" + integerField + integerField + realField
                          + realField + realField + realField
                          + " *$");
        Matcher matcher = regularPattern.matcher(line);
        if (matcher.matches()) {
          lineIndex++;
        }
        if (lineIndex==c&&c>2) {
          iKnow = true;
          fileIsOK = true;
        }
        if (lineIndex!=c&&c>2) {
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
    if (in==null) {
      throw new OrekitException("the reader has not been tested " , new String[0]);
    }
    if (fileIsOK==false) {
      throw new OrekitException("the reader is not adapted to the format " , new String[0]);
    }
    BufferedReader r = new BufferedReader(new InputStreamReader(in));
    Cl = new ArrayList(); 
    Sl = new ArrayList(); 
    for (String line = r.readLine();line!=null; line=r.readLine()) { 
      if(line.length()>=15) {
        String[] tab = line.trim().split("\\s+");
        
        fillArray(Integer.parseInt(tab[0]), Integer.parseInt(tab[1]), 
             Double.parseDouble(tab[2])*Math.sqrt(2*Integer.parseInt(tab[0])+1), 
             Double.parseDouble(tab[3])*Math.sqrt(2*Integer.parseInt(tab[0])+1));
      }  
    }
    C = (double[][])Cl.toArray(new double[Cl.size()][]);
    S = (double[][])Sl.toArray(new double[Sl.size()][]);
  }
  
  private void fillArray(int i, int j, double c, double s) {
    while(Cl.size()<=i) {
      double[] row = new double[Cl.size()+1];
      Cl.add(row);
    }
    while(Sl.size()<=i) {
      double[] row = new double[Sl.size()+1];
      Sl.add(row);
    }
    ((double[])Cl.get(i))[j] = c;
    ((double[])Sl.get(i))[j] = s;
  }
    
  /** Get the zonal coefficients.
   * @return J the zonal coefficients array.
   */
  public double[] getJ() {
    if (J==null) {
      for(int i = 0; i<C.length; i++){
        J[i]=-C[i][0];
      }
    }
    return J;
  }
  
  /** Get the tesseral-secorial and zonal coefficients. 
   * @return C the coefficients matrix
   */
  public double[][] getC() {
    return C;
  }
  
  /** Get the tesseral-secorial coefficients. 
   * @return S the coefficients matrix
   */
  public double[][] getS() {
    return S;
  }
  
  /** Get the value of mu associtated to the other coefficients.
   * @return mu (m³/s²)
   */
  public double getMu() {
    return mu;
  }
  
  /** Get the value of the Earth Equatorial Radius.
   * @return ae (m)
   */
  public double getAe() {
    return ae;
  }
  
  /** is file ok? */
  private boolean fileIsOK;
  
  /** The input to check and read */
  private InputStream in;
  
  /**  Earth Equatorial Radius */
  private double ae = 6378136.3;
  
  /** Mu */
  private double mu = 3986004.415E+8;
  
  /** zonal coefficients array */
  private double[] J;
  
  /** tesseral-secorial coefficients matrix */
  private double[][] C;
  
  /** tesseral-secorial coefficients matrix */
  private double[][] S;
  
  private ArrayList Cl;
  private ArrayList Sl; 
  
}
