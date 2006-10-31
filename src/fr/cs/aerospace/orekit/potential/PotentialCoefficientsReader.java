package fr.cs.aerospace.orekit.potential;

import java.io.IOException;
import java.io.InputStream;
import fr.cs.aerospace.orekit.errors.OrekitException;


/**This interface represents a Gravitationnal Potential Coefficients file reader.
 * 
 * <p> As it exits many different coefficients models and containers this
 *  interface represents all the methods that should be implemented by a reader.
 *  The proper way to use this interface is to call the 
 *  {@link fr.cs.aerospace.orekit.potential.PotentialReaderFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file <p>
 *   
 * @see fr.cs.aerospace.orekit.potential.PotentialReaderFactory
 * @author F. Maussion
 */
public interface PotentialCoefficientsReader {

  /** Check the file to determine if its format is understood by the reader or not.
   * @param in the input to check
   * @return true if it is readable, false if not.
   * @throws IOException when the {@link InputStream} cannot be buffered.
   */
  public boolean isFileOK(InputStream in) throws IOException ;

  /** Computes the coefficients by reading the selected (and tested) file 
   * @throws OrekitException when the file has not been initialized or checked.
   * @throws IOException when the file is corrupted.
   */
  public void read() throws OrekitException, IOException ;

  /** Get the fully normalized  zonal coefficients.
   * @return J the zonal coefficients array.
   */
  public double[] getNormJ();

  /** Get the fully normalized  zonal coefficients.
   * @param n the degree
   * @param m the order 
   * @return J the zonal coefficients array.
   */
  public double[] getNormJ(int n, int m);

  /** Get the fully normalized tesseral-secorial and zonal coefficients. 
   * @return C the coefficients matrix
   */
  public double[][] getNormC();

  /** Get the fully normalized tesseral-secorial and zonal coefficients.
   * @param n the degree
   * @param m the order 
   * @return C the coefficients matrix
   * @throws OrekitException 
   */
  public double[][] getNormC(int n, int m) throws OrekitException;

  /** Get the fully normalized tesseral-secorial coefficients. 
   * @return S the coefficients matrix
   */
  public double[][] getNormS();

  /** Get the fully normalized tesseral-secorial coefficients. 
   * @param n the degree
   * @param m the order 
   * @return S the coefficients matrix
   */
  public double[][] getNormS(int n, int m);

  /** Get the un-normalized  zonal coefficients.
   * @return J the zonal coefficients array.
   */
  public double[] getUnNormJ();

  /** Get the un-normalized  zonal coefficients.
   * @param n the degree
   * @param m the order
   * @return J the zonal coefficients array.
   */
  public double[] getUnNormJ(int n, int m );

  /** Get the un-normalized tesseral-secorial and zonal coefficients. 
   * @return C the coefficients matrix
   */
  public double[][] getUnNormC();

  /** Get the un-normalized tesseral-secorial and zonal coefficients. 
   * @param n the degree
   * @param m the order 
   * @return C the coefficients matrix
   */
  public double[][] getUnNormC(int n, int m );

  /** Get the un-normalized tesseral-secorial coefficients. 
   * @return S the coefficients matrix
   */
  public double[][] getUnNormS();

  /** Get the un-normalized tesseral-secorial coefficients. 
   * @param n the degree
   * @param m the order 
   * @return S the coefficients matrix
   */
  public double[][] getUnNormS(int n, int m );

  /** Get the value of mu associtated to the other coefficients.
   * @return mu (m³/s²)
   */
  public double getMu();

  /** Get the value of the Earth Equatorial Radius.
   * @return ae (m)
   */
  public double getAe();


}
