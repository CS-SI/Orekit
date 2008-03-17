package fr.cs.orekit.potential;

import java.io.IOException;
import java.io.InputStream;
import fr.cs.orekit.errors.OrekitException;

/**This abstract class represents a Gravitational Potential Coefficients file reader.
 *
 * <p> As it exits many different coefficients models and containers this
 *  interface represents all the methods that should be implemented by a reader.
 *  The proper way to use this interface is to call the
 *  {@link fr.cs.orekit.potential.PotentialReaderFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file.<p>
 *
 * @see fr.cs.orekit.potential.PotentialReaderFactory
 * @author F. Maussion
 */
public abstract class PotentialCoefficientsReader {

    /** Simple constructor.
     * <p>Build an uninitialized reader.</p>
     */
    protected PotentialCoefficientsReader() {
        normalizedJ = null;
        normalizedC = null;
        normalizedS = null;
        unNormalizedJ = null;
        unNormalizedC = null;
        unNormalizedS = null;
    }

    /** Check the file to determine if its format is understood by the reader or not.
     * @param in the input to check
     * @return true if it is readable, false if not.
     * @throws IOException when the {@link InputStream} cannot be buffered.
     */
    public abstract boolean isFileOK(InputStream in) throws IOException;

    /** Computes the coefficients by reading the selected (and tested) file
     * @throws OrekitException when the file has not been initialized or checked.
     * @throws IOException when the file is corrupted.
     */
    public abstract void read() throws OrekitException, IOException;

    /** Get the zonal coefficients.
     * @param normalized (true) or un-normalized (false)
     * @param n the maximal degree requested
     * @return J the zonal coefficients array.
     * @throws OrekitException
     */
    public double[] getJ(boolean normalized, int n) throws OrekitException {
        if (n >= normalizedC.length) {
            throw new OrekitException("too large degree (n = {0}), potential maximal degree is {1})",
                                      new Object[] {
                    new Integer(n),
                    new Integer(normalizedC.length - 1)
            });
        }

        double[] completeJ = normalized ? getNormalizedJ() : getUnNormalizedJ();

        // truncate the array as per caller request
        double[] result = new double[n + 1];
        System.arraycopy(completeJ, 0, result, 0, n + 1);

        return result;

    }

    /** Get the tesseral-sectorial and zonal coefficients.
     * @param n the degree
     * @param m the order
     * @param normalized (true) or un-normalized (false)
     * @return the cosines coefficients matrix
     * @throws OrekitException
     */
    public double[][] getC(int n, int m, boolean normalized)
    throws OrekitException {
        return truncateArray(n, m, normalized ? getNormalizedC()
                : getUnNormalizedC());
    }

    /** Get tesseral-sectorial coefficients.
     * @param n the degree
     * @param m the order
     * @param normalized (true) or un-normalized (false)
     * @return the sines coefficients matrix
     */
    public double[][] getS(int n, int m, boolean normalized)
    throws OrekitException {
        return truncateArray(n, m, normalized ? getNormalizedS()
                : getUnNormalizedS());
    }

    /** Get the tesseral-sectorial and zonal coefficients.
     * @param normalized (true) or un-normalized (false)
     * @param n the degree
     * @param m the order
     * @return C the coefficients matrix
     * @throws OrekitException
     */
    private double[][] truncateArray(int n, int m, double[][] complete)
    throws OrekitException {

        // safety checks
        if (n >= complete.length) {
            throw new OrekitException("too large degree (n = {0}), potential maximal degree is {1})",
                                      new Object[] {
                    new Integer(n),
                    new Integer(complete.length - 1)
            });
        }
        if (m >= complete[complete.length - 1].length) {
            throw new OrekitException("too large order (m = {0}), potential maximal order is {1})",
                                      new Object[] {
                    new Integer(m),
                    new Integer(complete[complete.length - 1].length - 1)
            });
        }

        // truncate each array row in turn
        double[][] result = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double[] ri = new double[Math.min(i, m) + 1];
            System.arraycopy(complete[i], 0, ri, 0, ri.length);
            result[i] = ri;
        }

        return result;

    }

    /** Get the fully normalized zonal coefficients.
     * @return J the zonal coefficients array.
     */
    private double[] getNormalizedJ() {
        if (normalizedJ == null) {
            normalizedJ = new double[normalizedC.length];
            for (int i = 0; i < normalizedC.length; i++) {
                normalizedJ[i] = -normalizedC[i][0];
            }
        }
        return normalizedJ;
    }

    /** Get the fully normalized tesseral-sectorial and zonal coefficients.
     * @return C the coefficients matrix
     */
    private double[][] getNormalizedC() {
        return normalizedC;
    }

    /** Get the fully normalized tesseral-sectorial coefficients.
     * @return S the coefficients matrix
     */
    private double[][] getNormalizedS() {
        return normalizedS;
    }

    /** Get the un-normalized  zonal coefficients.
     * @return J the zonal coefficients array.
     */
    private double[] getUnNormalizedJ() {
        if (unNormalizedJ == null) {
            double[][] uC = getUnNormalizedC();
            unNormalizedJ = new double[uC.length];
            for (int i = 0; i < uC.length; i++) {
                unNormalizedJ[i] = -uC[i][0];
            }
        }
        return unNormalizedJ;
    }

    /** Get the un-normalized tesseral-sectorial and zonal coefficients.
     * @return C the coefficients matrix
     */
    private double[][] getUnNormalizedC() {
        // calculate only if asked
        if (unNormalizedC == null) {
            unNormalizedC = unNormalize(normalizedC);
        }
        return unNormalizedC;
    }

    /** Get the un-normalized tesseral-sectorial coefficients.
     * @return S the coefficients matrix
     */
    private double[][] getUnNormalizedS() {
        // calculate only if asked
        if (unNormalizedS == null) {
            unNormalizedS = unNormalize(normalizedS);
        }
        return unNormalizedS;
    }

    /** Unnormalize a coefficients array.
     * @param normalized normalized coefficients array
     * @return unnormalized array
     */
    private double[][] unNormalize(double[][] normalized) {

        // allocate a triangular array
        double[][] unNormalized = new double[normalized.length][];
        unNormalized[0] = new double[] {
                normalized[0][0]
        };

        // initialization
        double factN = 1.0;
        double mfactNMinusM = 1.0;
        double mfactNPlusM = 1.0;

        // unnormalize the coefficients
        for (int n = 1; n < normalized.length; n++) {
            double[] uRow = new double[n + 1];
            double[] nRow = normalized[n];
            double coeffN = 2.0 * (2 * n + 1);
            factN *= n;
            mfactNMinusM = factN;
            mfactNPlusM = factN;
            uRow[0] = Math.sqrt(2 * n + 1) * normalized[n][0];
            for (int m = 1; m < nRow.length; m++) {
                mfactNPlusM *= (n + m);
                mfactNMinusM /= (n - m + 1);
                uRow[m] = Math.sqrt((coeffN * mfactNMinusM) / mfactNPlusM) * nRow[m];
            }
            unNormalized[n] = uRow;
        }

        return unNormalized;

    }

    /** Get the value of mu associtated to the other coefficients.
     * @return mu (m<sup>3</sup>/s<sup>2</sup>)
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

    /** fully normalized zonal coefficients array */
    protected double[] normalizedJ;

    /** fully normalized tesseral-sectorial coefficients matrix */
    protected double[][] normalizedC;

    /** fully normalized tesseral-sectorial coefficients matrix */
    protected double[][] normalizedS;

    /** un-normalized zonal coefficients array */
    private double[] unNormalizedJ;

    /** un-normalized tesseral-sectorial coefficients matrix */
    private double[][] unNormalizedC;

    /** un-normalized tesseral-sectorial coefficients matrix */
    private double[][] unNormalizedS;

    /** Earth Equatorial Radius */
    protected double ae;

    /** Mu */
    protected double mu;

}
