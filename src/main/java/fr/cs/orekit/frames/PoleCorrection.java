package fr.cs.orekit.frames;

import java.io.Serializable;

/** Simple container class for pole correction parameters.
 * <p>This class is a simple container, it does not provide
 * any processing method.</p>
 * @author Luc Maisonobe
 */
public class PoleCorrection implements Serializable {

    /** Null correction (xp = 0, yp = 0). */
    public static final PoleCorrection NULL_CORRECTION =
        new PoleCorrection(0, 0);

    /** Serializable UID. */
    private static final long serialVersionUID = 8695216598525302806L;

    /** x<sub>p</sub> parameter (radians). */
    private final double xp;

    /** y<sub>p</sub> parameter (radians). */
    private final double yp;

    /** Simple constructor.
     * @param xp x<sub>p</sub> parameter (radians)
     * @param yp y<sub>p</sub> parameter (radians)
     */
    public PoleCorrection(final double xp, final double yp) {
        this.xp = xp;
        this.yp = yp;
    }

    /** Get the x<sub>p</sub> parameter.
     * @return x<sub>p</sub> parameter
     */
    public double getXp() {
        return xp;
    }

    /** Get the y<sub>p</sub> parameter.
     * @return y<sub>p</sub> parameter
     */
    public double getYp() {
        return yp;
    }

}
