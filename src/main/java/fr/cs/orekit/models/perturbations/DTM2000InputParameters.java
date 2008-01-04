package fr.cs.orekit.models.perturbations;

import fr.cs.orekit.time.AbsoluteDate;

/** Container for solar activity datas, compatible with DTM2000 Atmosphere model.
 *
 * This model needs mean and instantaneous solar flux and geomagnetic incides to
 * compute the local density. Mean solar flux is (for the moment) represented by
 * the F10.7 indices. Instantaneous flux can be setted to the mean value if the
 * data is not available. Geomagnetic acivity is represented by the Kp indice,
 * which goes from 1 (very low activity) to 9 (high activity).
 * <p>
 * All needed solar activity datas can be found on the <a
 * href="http://sec.noaa.gov/Data/index.html">
 * NOAA (National Oceanic and Atmospheric
 * Administration) website.</a>
 *</p>
 *
 * @author F. Maussion
 */
public interface DTM2000InputParameters {

  /** Gets the available data range minimum date.
   * @return the minimum date.
   */
  public AbsoluteDate getMinDate();

  /** Gets the available data range maximum date.
   * @return the maximum date.
   */
  public AbsoluteDate getMaxDate();

  /** Get the value of the instantaneous solar flux.
   * @param date the current date
   * @return the instantaneous solar flux
   */
  public double getInstantFlux(AbsoluteDate date);

  /** Get the value of the mean solar flux.
   * @param date the current date
   * @return the mean solar flux
   */
  public double getMeanFlux(AbsoluteDate date);

  /** Get the value of the 3H geomagnetic index.
   * With a delay of 3 hr at pole to 6 hr at equator using:
   * delay=6-abs(lat)*0.033 (lat in deg.)
   * @param date the current date
   * @return the 3H geomagnetic index
   */
  public double getThreeHourlyKP(AbsoluteDate date);

  /** Get the last 24H mean geomagnetic index.
   * @param date the current date
   * @return the 24H geomagnetic index
   */
  public double get24HoursKp(AbsoluteDate date);


}
