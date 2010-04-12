package org.orekit.forces;

import java.util.Collection;

/** This interface enables the parameters jacobian processing.
 *
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */

public interface Parameterizable {

    /** Get the names of the supported parameters.
     * @return parameters names
     */
    Collection<String> getParametersNames();

    /** Get parameter value from its name.
     * @param name parameter name
     * @return parameter value
     * @exception IllegalArgumentException if parameter is not supported
     */
    double getParameter(String name) throws IllegalArgumentException;

    /** Set the value for a given parameter.
     * @param name parameter name
     * @param value parameter value
     * @exception IllegalArgumentException if parameter is not supported
     */
    void setParameter(String name, double value) throws IllegalArgumentException;

}
