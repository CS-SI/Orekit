package fr.cs.orekit.models.perturbations;

import java.io.Serializable;

import fr.cs.orekit.time.AbsoluteDate;

/**  Interface for solar activity and magnetic activity data.
 * @author F. Maussion
 */
public interface JB2006InputParameters extends Serializable {

    /** Gets the available data range minimum date.
     * @return the minimum date.
     */
    public AbsoluteDate getMinDate();

    /** Gets the available data range maximum date.
     * @return the maximum date.
     */
    public AbsoluteDate getMaxDate();

    /** Get the value of the instantaneous solar flux F10.7 index
     *        (1e<sup>-22</sup>*Watt/(m<sup>2</sup>*Hertz))
     *                 (Tabular time 1.0 day earlier).
     * @param date the current date
     * @return the instantaneous F10.7 index
     */
    public double getF10(AbsoluteDate date);

    /** Get the value of the mean solar flux,
     *    averaged 81-day centered F10.7 B indexon the input time.
     * @param date the current date
     * @return the mean solar flux F10.7B index
     */
    public double getF10B(AbsoluteDate date);

    /** Get the EUV index (26-34 nm) scaled to F10. Tabular time 1 day earlier.
     * @param date the current date
     * @return the the EUV S10 index
     */
    public double getS10(AbsoluteDate date);

    /** Get the EUV 81-day averaged centered index
     * @param date the current date
     * @return the the mean EUV S10B index
     */
    public double getS10B(AbsoluteDate date);

    /** Get the MG2 index scaled to F10
     * @param date the current date
     * @return the the EUV S10 index
     */
    public double getXM10(AbsoluteDate date);

    /** Get the MG2 81-day ave. centered index. Tabular time 5.0 days earlier.
     * @param date the current date
     * @return the the mean EUV S10B index
     */
    public double getXM10B(AbsoluteDate date);

    /** Get the Geomagnetic planetary 3-hour index A<sub>p</sub>
     *            for a tabular time 6.7 hours earlier
     * @param date the current date
     * @return the A<sub>p</sub> index
     */
    public double getAp(AbsoluteDate date);


}
