package org.orekit.propagation.semianalytical.dsst.coefficients;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.regex.Pattern;

import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;

/**
 * {@link ModifiedNewcombOperators} loader. Unimplemented yet.
 * 
 * @author rdicosta
 */
public class ModifiedNewcombOperatorLoader implements DataLoader {

    /** Supported file pattern */
    private static final String PATTERN = "^ModifidedNewcombOperator\\.txt$";

    /** maximum frequency index used in the tesseral perturbation */
    private int                 maxFreq;

    /** maximum resonant degree available */
    private int                 resonantDegree;

    /** maximum d'Alembert characteristic */
    private int                 alembertMax;

    /**
     * maximum power of eccentricity used in the expansion of the Hansen coefficient Kernel
     */
    private int                 powerOfE;

    /**
     * @param maxFreq
     *            maximum frequency index used in the tesseral perturbation
     * @param resonantDegree
     *            maximum resonant degree available
     * @param alembertMax
     *            maximum d'Alembert characteristic
     * @param powerOfE
     *            maximum power of eccentricity used in the expansion of the Hansen coefficient
     *            Kernel
     */
    public ModifiedNewcombOperatorLoader(final int maxFreq,
                                         final int resonantDegree,
                                         final int alembertMax,
                                         final int powerOfE) {
        this.maxFreq = maxFreq;
        this.resonantDegree = resonantDegree;
        this.alembertMax = alembertMax;
        this.powerOfE = powerOfE;

        // double pattern
        final String doublePattern = "\\p{Blank}*(-?\\p{Digit}*\\.\\p{Digit}*)";

        // integer pattern
        final String intPattern = "((?:\\p{Blank}|\\p{Digit})\\p{Digit})";
        Pattern regularPattern = Pattern.compile(intPattern + intPattern + intPattern + intPattern + intPattern + doublePattern);

        // this.entries = new TreeMap<Integer>();

    }

    /**
     * 
     */
    public final boolean stillAcceptsData() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void loadData(final InputStream input,
                         final String name) throws IOException, ParseException, OrekitException {

    }

}
