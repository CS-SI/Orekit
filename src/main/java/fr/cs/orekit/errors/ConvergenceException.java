package fr.cs.orekit.errors;

public class ConvergenceException
extends OrekitException {

    /** Serializable UID. */
    private static final long serialVersionUID = -8509280851120696283L;

    /** Simple constructor.
     * @param iterations number of iterations already realized
     */
    public ConvergenceException(int iterations) {
        super("unable to converge after {0} iterations",
              new Object[] { new Integer(iterations) });
    }

}
