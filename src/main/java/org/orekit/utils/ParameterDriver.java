/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.events.ParameterDrivenDateIntervalDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeSpanMap.Transition;


/** Class allowing to drive the value of a parameter.
 * <p>
 * This class is typically used as a bridge between an estimation
 * algorithm (typically orbit determination or optimizer) and an
 * internal parameter in a physical model that needs to be tuned,
 * or a bridge between a finite differences algorithm and an
 * internal parameter in a physical model that needs to be slightly
 * offset. The physical model will expose to the algorithm a
 * set of instances of this class so the algorithm can call the
 * {@link #setValue(double, AbsoluteDate)} method to update the
 * parameter value at a given date. Some parameters driver only have 1 value estimated/driven
 * over the all period (constructor by default). Some others have several
 * values estimated/driven on several periods/intervals. For example if the time period is 3 days
 * for a drag parameter estimated all days then 3 values would be estimated, one for
 * each time period. In order to allow several values to be estimated, the PDriver has
 * a name and a value {@link TimeSpanMap} as attribute. In order,
 * to cut the time span map there are 2 options :
 * </p>
 * <ul>
 * <li>Passive cut calling the {@link #addSpans(AbsoluteDate, AbsoluteDate, double)} method.
 * Given a start date, an end date and and a validity period (in sec)
 * for the driver, the {@link #addSpans} method will cut the interval of name and value time span map
 * from start date to date end in several interval of validity period duration. This method should not
 * be called on orbital drivers and must be called only once at beginning of the process (for example
 * beginning of orbit determination). <b>WARNING : In order to ensure converge for orbit determination,
 * the start, end date and driver periodicity must be wisely choosen </b>. There must be enough measurements
 * on each interval or convergence won't reach or singular matrixes will appear.  </li>
 * <li> Active cut calling the {@link #addSpanAtDate(AbsoluteDate)} method.
 * Given a date, the method will cut the value and name time span name, in order to have a new span starting at
 * the given date. Can be called several time to cut the time map as wished. <b>WARNING : In order to ensure
 * converge for orbit determination, if the method is called several time, the start date must be wisely choosen </b>.
 * There must be enough measurements on each interval or convergence won't reach or singular matrixes will appear.  </li>
 * </ul>
 * <p>
 * Several ways exist in order to get a ParameterDriver value at a certain
 * date for parameters having several values on several intervals.
 * </p>
 * <ul>
 * <li>First of all, the step estimation, that is to say, if a value wants
 * to be known at a certain date, the value returned is the one of span
 * beginning corresponding to the date. With this definition a value
 * will be kept all along the span duration and will be the value of the span
 * start.</li>
 * <li> The continuous estimation, that is to say, when a value wants be to
 * known at a date t, the value returned would be a linear interpolation between
 * the value at the beginning of the span corresponding to date t and end this span
 * (which is also the beginning of next span). NOT IMPLEMENTED FOR NOW
 * </li>
 * </ul>
 * Each time the value is set, the physical model
 * will be notified as it will register a {@link ParameterObserver
 * ParameterObserver} for this purpose.
 * <p>
 * This design has two major goals. First, it allows an external
 * algorithm to drive internal parameters almost anonymously, as it only
 * needs to get a list of instances of this class, without knowing
 * what they really drive. Second, it allows the physical model to
 * not expose directly setters methods for its parameters. In order
 * to be able to modify the parameter value, the algorithm
 * <em>must</em> retrieve a parameter driver.
 * </p>
 * @see ParameterObserver
 * @author Luc Maisonobe
 * @author Melina Vanel
 * @since 8.0
 */
public class ParameterDriver {

    /** Name of the parameter.*/
    private String SPAN = "Span";

    /** Name of the parameter. */
    private String name;

    /** TimeSpan for period names.
     * @since 12.0
     */
    private TimeSpanMap<String> nameSpanMap;

    /** Reference value. */
    private double referenceValue;

    /** Scaling factor. */
    private double scale;

    /** Minimum value. */
    private double minValue;

    /** Maximum value. */
    private double maxValue;

    /** Reference date.
     * @since 9.0
     */
    private AbsoluteDate referenceDate;

    /** Flag to choose estimation method. If estimationContinuous
     * is true then when a value wants to be known an interpolation
     * is performed between given date span start and end (start of
     * next span) otherwise the value returned is the value of span start
     * @since 12.0
     */
    private boolean isEstimationContinuous;

    /** Value time span map.
     * @since 12.0
     */
    private TimeSpanMap<Double> valueSpanMap;

    /** Selection status.
     * <p>
     * Selection is used for estimated parameters in orbit determination,
     * or to compute the Jacobian matrix in partial derivatives computation.
     * </p>
     */
    private boolean selected;

    /** Observers observing this driver. */
    private final List<ParameterObserver> observers;

    /** Create a new instance from another parameterDriver informations
     * for example (useful for {@link ParameterDriversList.DelegatingDriver}))
     * At construction, the parameter new is configured as <em>not</em> selected,
     * the reference date is set to {@code null}. validityPeriod, namesSpanMap and
     * valueSpanMap.
     * @param name general name of the parameter
     * @param namesSpanMap name time span map. WARNING, number of Span must be coherent with
     * validityPeriod and valueSpanMap (same number of Span with same transitions
     * dates)
     * @param valuesSpanMap values time span map
     * @param referenceValue reference value of the parameter
     * @param scale scaling factor to convert the parameters value to
     * non-dimensional (typically set to the expected standard deviation of the
     * parameter), it must be non-zero
     * @param minValue minimum value allowed
     * @param maxValue maximum value allowed
     * @since 12.0
     */
    public ParameterDriver(final String name, final TimeSpanMap<String> namesSpanMap,
                           final TimeSpanMap<Double> valuesSpanMap, final double referenceValue,
                           final double scale, final double minValue, final double maxValue) {
        if (FastMath.abs(scale) <= Precision.SAFE_MIN) {
            throw new OrekitException(OrekitMessages.TOO_SMALL_SCALE_FOR_PARAMETER,
                                      name, scale);
        }
        this.name                   = name;
        this.nameSpanMap            = namesSpanMap;
        this.referenceValue         = referenceValue;
        this.scale                  = scale;
        this.minValue               = minValue;
        this.maxValue               = maxValue;
        this.referenceDate          = null;
        this.valueSpanMap           = valuesSpanMap;
        this.selected               = false;
        this.observers              = new ArrayList<>();
        this.isEstimationContinuous = false;
    }

    /** Simple constructor.
     * <p>
     * At construction, the parameter is configured as <em>not</em> selected,
     * the reference date is set to {@code null}, the value is set to the
     * {@code referenceValue}, the validity period is set to 0 so by default
     * the parameterDriver will be estimated on only 1 interval from -INF to
     * +INF. To change the validity period the
     * {@link ParameterDriver#addSpans(AbsoluteDate, AbsoluteDate, double)}
     * method must be called.
     * </p>
     * @param name name of the parameter
     * @param referenceValue reference value of the parameter
     * @param scale scaling factor to convert the parameters value to
     * non-dimensional (typically set to the expected standard deviation of the
     * parameter), it must be non-zero
     * @param minValue minimum value allowed
     * @param maxValue maximum value allowed
     */
    public ParameterDriver(final String name,
                           final double referenceValue, final double scale,
                           final double minValue, final double maxValue) {
        if (FastMath.abs(scale) <= Precision.SAFE_MIN) {
            throw new OrekitException(OrekitMessages.TOO_SMALL_SCALE_FOR_PARAMETER,
                                      name, scale);
        }
        this.name                   = name;
        this.nameSpanMap            = new TimeSpanMap<>(SPAN + name + Integer.toString(0));
        this.referenceValue         = referenceValue;
        this.scale                  = scale;
        this.minValue               = minValue;
        this.maxValue               = maxValue;
        this.referenceDate          = null;
        // at construction the parameter driver
        // will be consider with only 1 estimated value over the all orbit
        // determination
        this.valueSpanMap           = new TimeSpanMap<>(referenceValue);
        this.selected               = false;
        this.observers              = new ArrayList<>();
        this.isEstimationContinuous = false;
    }

    /** Get current name span map of the parameterDriver, cut in interval
     * in accordance with value span map and validity period.
     * @return current name span map
     * @since 12.0
     */
    public TimeSpanMap<String> getNamesSpanMap() {
        return nameSpanMap;
    }

    /** Get value time span map for parameterDriver.
     * @return value time span map
     * @since 12.0
     */
    public TimeSpanMap<Double> getValueSpanMap() {
        return valueSpanMap;
    }

    /** Set current parameter value span map to match another driver. In order to keep
     * consistency, the validity period and name span map are updated.
     * @param driver for which the value span map wants to be copied for the
     * current driver
     * @since 12.0
     */
    public void setValueSpanMap(final ParameterDriver driver) {
        final TimeSpanMap<Double> previousValueSpanMap = driver.getValueSpanMap();
        valueSpanMap   = driver.getValueSpanMap();
        nameSpanMap    = driver.getNamesSpanMap();
        for (final ParameterObserver observer : observers) {
            observer.valueSpanMapChanged(previousValueSpanMap, this);
        }
    }

    /** Get the number of values to estimate that is to say the number.
     * of Span present in valueSpanMap
     * @return int the number of values to estimate
     * @since 12.0
     */
    public int getNbOfValues() {
        return valueSpanMap.getSpansNumber();
    }

    /** Get the dates of the transitions for the drag sensitive models {@link TimeSpanMap}.
     * @return dates of the transitions for the drag sensitive models {@link TimeSpanMap}
     * @since 12.0
     */
    public AbsoluteDate[] getTransitionDates() {

        // Get all transitions
        final List<AbsoluteDate> listDates = new ArrayList<>();

        // Extract all the transitions' dates
        for (Transition<Double> transition = getValueSpanMap().getFirstSpan().getEndTransition(); transition != null; transition = transition.next()) {
            listDates.add(transition.getDate());
        }
        // Return the array of transition dates
        return listDates.toArray(new AbsoluteDate[0]);
    }

    /** Get all values of the valueSpanMap in the chronological order.
     * @return double[] containing values of the valueSpanMap in the chronological order
     */
    public double[] getValues() {
        final double[] chronologicalValues = new double[getNbOfValues()];
        Span<Double> currentSpan = valueSpanMap.getFirstSpan();
        for (int i = 0; i < getNbOfValues() - 1; i++) {
            chronologicalValues[i] = currentSpan.getData();
            currentSpan = currentSpan.next();
        }
        chronologicalValues[getNbOfValues() - 1 ] = currentSpan.getData();
        return chronologicalValues;
    }


    /** Add an observer for this driver.
     * <p>
     * The observer {@link ParameterObserver#valueSpanMapChanged(TimeSpanMap, ParameterDriver)
     * valueSpanMapChanged} method is called once automatically when the
     * observer is added, and then called at each value change.
     * </p>
     * @param observer observer to add
          * while being updated
     */
    public void addObserver(final ParameterObserver observer) {
        observers.add(observer);
        observer.valueSpanMapChanged(getValueSpanMap(), this);
    }

    /** Remove an observer.
     * @param observer observer to remove
     * @since 9.1
     */
    public void removeObserver(final ParameterObserver observer) {
        for (final Iterator<ParameterObserver> iterator = observers.iterator(); iterator.hasNext();) {
            if (iterator.next() == observer) {
                iterator.remove();
                return;
            }
        }
    }

    /** Replace an observer.
     * @param oldObserver observer to replace
     * @param newObserver new observer to use
     * @since 10.1
     */
    public void replaceObserver(final ParameterObserver oldObserver, final ParameterObserver newObserver) {
        for (int i = 0; i < observers.size(); ++i) {
            if (observers.get(i) == oldObserver) {
                observers.set(i, newObserver);
            }
        }
    }

    /** Get the observers for this driver.
     * @return an unmodifiable view of the observers for this driver
     * @since 9.1
     */
    public List<ParameterObserver> getObservers() {
        return Collections.unmodifiableList(observers);
    }

    /** Get parameter driver general name.
     * @return name
     */
    public String getName() {
        return name;
    }

    /** Get name of the parameter span for a specific date.
     * @param date date at which the name of the span wants to be known
     * @return name data of the name time span map at date
     */
    public String getNameSpan(final AbsoluteDate date) {
        return nameSpanMap.get(date);
    }

    /** Change the general name of this parameter driver.
     * @param name new name
     */
    public void setName(final String name) {
        final String previousName = this.name;
        this.name = name;
        for (final ParameterObserver observer : observers) {
            observer.nameChanged(previousName, this);
        }
        // the names time span map must also be updated with the new name
        if (nameSpanMap.getSpansNumber() > 1) {
            Span<String> currentNameSpan = nameSpanMap.getFirstSpan();
            nameSpanMap.addValidBefore(SPAN + name + Integer.toString(0), currentNameSpan.getEnd(), false);
            for (int spanNumber = 1; spanNumber < nameSpanMap.getSpansNumber(); ++spanNumber) {
                currentNameSpan = nameSpanMap.getSpan(currentNameSpan.getEnd());
                nameSpanMap.addValidAfter(SPAN + name + Integer.toString(spanNumber), currentNameSpan.getStart(), false);
            }
        } else {
            nameSpanMap = new TimeSpanMap<>(SPAN + name + Integer.toString(0));
        }
    }

    /** Cut values and names time span map given orbit determination start and end and driver
     * periodicity.
     * <p>
     * For example for a drag coefficient the validity period would be
     * 1 days = 86400sec. To be called after constructor to cut the temporal axis with
     * the wanted parameter driver temporality for estimations on the wanted interval.
     * </p>
     * <p>
     * Must be called only once at the beginning of orbit
     * determination for example. If called several times, will throw exception. If parameter
     * estimations intervals must be changed then a new ParameterDriver must be created or the
     * function {@link #addSpanAtDate} should be used.
     * </p>
     * <p>
     * This function should not be called on {@link DateDriver} and
     * any of {@link ParameterDrivenDateIntervalDetector} attribute, because there is no sense to
     * estimate several values for dateDriver.
     * </p>
     * <p>
     * The choice of {@code orbitDeterminationStartDate}, {@code orbitDeterminationEndDate} and
     * {@code validityPeriodForDriver} in a case of orbit determination must be done carefully,
     * indeed, enough measurement should be available for each time interval or
     * the orbit determination won't converge.
     * </p>
     * @param orbitDeterminationStartDate start date for which the parameter driver
     * starts to be estimated.
     * @param orbitDeterminationEndDate end date for which the parameter driver
     * stops to be estimated.
     * @param validityPeriodForDriver validity period for which the parameter value
     * is effective (for example 1 day for drag coefficient). Warning, validityPeriod
     * should not be too short or the orbit determination won't converge.
     * @since 12.0
     */
    public void addSpans(final AbsoluteDate orbitDeterminationStartDate,
                         final AbsoluteDate orbitDeterminationEndDate,
                         final double validityPeriodForDriver) {

        // by convention 0 is when the parameter needs to be drived only on 1
        // interval from -INF to +INF time period
        if (getNbOfValues() != 1) {
            // throw exception if called several time, must be called only once at the beginning of orbit
            // determination, if the periods wants to be changed a new parameter must be created
            throw new OrekitIllegalStateException(OrekitMessages.PARAMETER_PERIODS_HAS_ALREADY_BEEN_SET, name);
        } else {

            int spanNumber = 1;
            AbsoluteDate currentDate = orbitDeterminationStartDate.shiftedBy(validityPeriodForDriver);
            //splitting the names and values span map accordingly with start and end of orbit determination
            //and validity period. A security is added to avoid having to few measurements point for a span
            //in order to assure orbit determination convergence
            while (currentDate.isBefore(orbitDeterminationEndDate) && orbitDeterminationEndDate.durationFrom(currentDate) > validityPeriodForDriver / 3.0) {
                valueSpanMap.addValidAfter(getValue(currentDate), currentDate, false);
                nameSpanMap.addValidAfter(SPAN + getName() + Integer.toString(spanNumber++), currentDate, false);
                currentDate = currentDate.shiftedBy(validityPeriodForDriver);
            }
        }
    }

    /** Create a new span in values and names time span map given a start date.
     * <b> One must be aware of the importance of choosing wise dates if this function is called
     * several times to create several span at wanted times. Indeed, if orbit determination is performed
     * it might not converge or find singular matrix if the spans are too short and contains to few measurements.
     * Must be called before any computation (for example before
     * orbit determination).</b>
     * @param spanStartDate wanted start date for parameter value interval
     * starts to be estimated.
     * @since 12.0
     */
    public void addSpanAtDate(final AbsoluteDate spanStartDate) {

        // Split value span map with new interval having for start date spanStartDate and end
        // date next span start date of +INF if no span is present after
        valueSpanMap.addValidAfter(getValue(spanStartDate), spanStartDate, false);
        nameSpanMap.addValidAfter(name, spanStartDate, false);
        // Rename spans recursively
        Span<String> currentNameSpan = nameSpanMap.getFirstSpan();
        nameSpanMap.addValidBefore(SPAN + name + Integer.toString(0), currentNameSpan.getEnd(), false);

        for (int spanNumber = 1; spanNumber < nameSpanMap.getSpansNumber(); spanNumber++) {
            currentNameSpan = nameSpanMap.getSpan(currentNameSpan.getEnd());
            nameSpanMap.addValidAfter(SPAN + name + Integer.toString(spanNumber), currentNameSpan.getStart(), false);
        }
    }

    /** Get reference parameter value.
     * @return reference parameter value
     */
    public double getReferenceValue() {
        return referenceValue;
    }

    /** Set reference parameter value.
     * @since 9.3
     * @param referenceValue the reference value to set.
     */
    public void setReferenceValue(final double referenceValue) {
        final double previousReferenceValue = this.referenceValue;
        this.referenceValue = referenceValue;
        for (final ParameterObserver observer : observers) {
            observer.referenceValueChanged(previousReferenceValue, this);
        }
    }

    /** Get minimum parameter value.
     * @return minimum parameter value
     */
    public double getMinValue() {
        return minValue;
    }

    /** Set minimum parameter value.
     * @since 9.3
     * @param minValue the minimum value to set.
     */
    public void setMinValue(final double minValue) {
        final double previousMinValue = this.minValue;
        this.minValue = minValue;
        for (final ParameterObserver observer : observers) {
            observer.minValueChanged(previousMinValue, this);
        }
        // Check if all values are still not out of min/max range
        for (Span<Double> span = valueSpanMap.getFirstSpan(); span != null; span = span.next()) {
            setValue(getValue(span.getStart()), span.getStart());
        }
    }

    /** Get maximum parameter value.
     * @return maximum parameter value
     */
    public double getMaxValue() {
        return maxValue;
    }

    /** Set maximum parameter value.
     * @since 9.3
     * @param maxValue the maximum value to set.
     */
    public void setMaxValue(final double maxValue) {
        final double previousMaxValue = this.maxValue;
        this.maxValue = maxValue;
        for (final ParameterObserver observer : observers) {
            observer.maxValueChanged(previousMaxValue, this);
        }
        // Check if all values are still not out of min/max range
        for (Span<Double> span = valueSpanMap.getFirstSpan(); span != null; span = span.next()) {
            setValue(getValue(span.getStart()), span.getStart());
        }
    }

    /** Get scale.
     * @return scale
     */
    public double getScale() {
        return scale;
    }

    /** Set scale.
     * @since 9.3
     * @param scale the scale to set.
     */
    public void setScale(final double scale) {
        final double previousScale = this.scale;
        this.scale = scale;
        for (final ParameterObserver observer : observers) {
            observer.scaleChanged(previousScale, this);
        }
    }

    /** Get normalized value at specific date.
     * <p>
     * The normalized value is a non-dimensional value
     * suitable for use as part of a vector in an optimization
     * process. It is computed as {@code (current - reference)/scale}.
     * </p>
     * @param date date for which the normalized value wants to be known
     * @return normalized value
     */
    public double getNormalizedValue(final AbsoluteDate date) {
        return (getValue(date) - getReferenceValue()) / scale;
    }

    /** Get normalized value. Only useable on ParameterDriver
     * which have only 1 span on their TimeSpanMap value (that is
     * to say for which the setPeriod method wasn't called) otherwise
     * it will throw an exception.
     * <p>
     * The normalized value is a non-dimensional value
     * suitable for use as part of a vector in an optimization
     * process. It is computed as {@code (current - reference)/scale}.
     * </p>
     * @return normalized value
     */
    public double getNormalizedValue() {
        return (getValue() - getReferenceValue()) / scale;
    }

    /** Set normalized value at specific date.
     * <p>
     * The normalized value is a non-dimensional value
     * suitable for use as part of a vector in an optimization
     * process. It is computed as {@code (current - reference)/scale}.
     * </p>
     * @param date date for which the normalized value wants to be set
     * @param normalized value
     */
    public void setNormalizedValue(final double normalized, final AbsoluteDate date) {
        setValue(getReferenceValue() + scale * normalized, date);
    }

    /** Set normalized value at specific date. Only useable on ParameterDriver
     * which have only 1 span on their TimeSpanMap value (that is
     * to say for which the setPeriod method wasn't called) otherwise
     * it will throw an exception.
     * <p>
     * The normalized value is a non-dimensional value
     * suitable for use as part of a vector in an optimization
     * process. It is computed as {@code (current - reference)/scale}.
     * </p>
     * @param normalized value
     */
    public void setNormalizedValue(final double normalized) {
        setValue(getReferenceValue() + scale * normalized);
    }

    /** Get current reference date.
     * @return current reference date (null if it was never set)
     * @since 9.0
     */
    public AbsoluteDate getReferenceDate() {
        return referenceDate;
    }

    /** Set reference date.
     * @param newReferenceDate new reference date
     * @since 9.0
     */
    public void setReferenceDate(final AbsoluteDate newReferenceDate) {
        final AbsoluteDate previousReferenceDate = getReferenceDate();
        referenceDate = newReferenceDate;
        for (final ParameterObserver observer : observers) {
            observer.referenceDateChanged(previousReferenceDate, this);
        }
    }

    /** Get current parameter value. Only usable on ParameterDriver
     * which have only 1 span on their TimeSpanMap value (that is
     * to say for which the setPeriod method wasn't called)
     * @return current parameter value
     */
    public double getValue() {
        if (getNbOfValues() > 1) {
            throw new OrekitIllegalStateException(OrekitMessages.PARAMETER_WITH_SEVERAL_ESTIMATED_VALUES, name, "getValue(date)");
        }
        // Attention voir si qlqchose est retourné si une exception est levée
        return valueSpanMap.getFirstSpan().getData();
    }

    /** Get current parameter value at specific date, depending on isContinuousEstimation
     * value, the value returned will be obtained by step estimation or continuous estimation.
     * @param date date for which the value wants to be known. Only if
     * parameter driver has 1 value estimated over the all orbit determination
     * period (not validity period intervals for estimation), the date value can
     * be <em>{@code null}</em> and then the only estimated value will be
     * returned, in this case the date can also be whatever the value returned would
     * be the same. Moreover in this particular case one can also call the {@link #getValue()}.
     * @return current parameter value at date date, or for the all period if
     * no validity period (= 1 value estimated over the all orbit determination
     * period)
     */
    public double getValue(final AbsoluteDate date) {
        return isEstimationContinuous ? getValueContinuousEstimation(date) : getValueStepEstimation(date);
    }

    /** Get current parameter value at specific date with step estimation.
     * @param date date for which the value wants to be known. Only if
     * parameter driver has 1 value estimated over the all orbit determination
     * period (not validity period intervals for estimation), the date value can
     * be <em>{@code null}</em> and then the only estimated value will be
     * returned, in this case the date can also be whatever the value returned would
     * be the same. Moreover in this particular case one can also call the {@link #getValue()}.
     * @return current parameter value at date date, or for the all period if
     * no validity period (= 1 value estimated over the all orbit determination
     * period)
     */
    public double getValueStepEstimation(final AbsoluteDate date) {
        return getNbOfValues() == 1 ? valueSpanMap.getFirstSpan().getData() : valueSpanMap.get(date);
    }

    /** Get current parameter value at specific date with continuous estimation.
     * @param date date for which the value wants to be known. Only if
     * parameter driver has 1 value estimated over the all orbit determination
     * period (not validity period intervals for estimation), the date value can
     * be <em>{@code null}</em> and then the only estimated value will be
     * returned, in this case the date can also be whatever the value returned would
     * be the same. Moreover in this particular case one can also call the {@link #getValue()}.
     * @return current parameter value at date date, or for the all period if
     * no validity period (= 1 value estimated over the all orbit determination
     * period)
     * @since 12.0
     */
    public double getValueContinuousEstimation(final AbsoluteDate date) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /** Get the value as a gradient at special date.
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the differentiation parameters in derivatives computations
     * @return value with derivatives, will throw exception if called on a PDriver having
     * several values driven
     * @since 10.2
     */
    public Gradient getValue(final int freeParameters, final Map<String, Integer> indices) {
        Integer index = null;
        for (Span<String> span = nameSpanMap.getFirstSpan(); span != null; span = span.next()) {
            index = indices.get(span.getData());
            if (index != null) {
                break;
            }
        }
        return (index == null) ? Gradient.constant(freeParameters, getValue()) : Gradient.variable(freeParameters, index, getValue());
    }

    /** Get the value as a gradient at special date.
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the differentiation parameters in derivatives computations,
     * must be span name and not driver name
     * @param date date for which the value wants to be known. Only if
     * parameter driver has 1 value estimated over the all orbit determination
     * period (not validity period intervals for estimation), the date value can
     * be <em>{@code null}</em> and then the only estimated value will be
     * returned
     * @return value with derivatives
     * @since 10.2
     */
    public Gradient getValue(final int freeParameters, final Map<String, Integer> indices, final AbsoluteDate date) {
        Integer index = null;
        for (Span<String> span = nameSpanMap.getFirstSpan(); span != null; span = span.next()) {
            index = indices.get(span.getData());
            if (index != null) {
                break;
            }
        }
        return (index == null) ? Gradient.constant(freeParameters, getValue(date)) : Gradient.variable(freeParameters, index, getValue(date));
    }

    /** Set parameter value at specific date.
     * <p>
     * If {@code newValue} is below {@link #getMinValue()}, it will
     * be silently set to {@link #getMinValue()}. If {@code newValue} is
     * above {@link #getMaxValue()}, it will be silently set to {@link
     * #getMaxValue()}.
     * </p>
     * @param date date for which the value wants to be set. Only if
     * parameter driver has 1 value estimated over the all orbit determination
     * period (not validity period intervals for estimation), the date value can
     * be <em>{@code null}</em>
     * @param newValue new value to set
     */
    public void setValue(final double newValue, final AbsoluteDate date) {

        double previousValue = Double.NaN;
        AbsoluteDate referenceDateSpan = AbsoluteDate.ARBITRARY_EPOCH;

        // if valid for infinity (only 1 value estimation for the orbit determination )
        if (getNbOfValues() == 1) {
            previousValue = this.getValue(referenceDateSpan);
            this.valueSpanMap = new TimeSpanMap<>(FastMath.max(minValue, FastMath.min(maxValue, newValue)));
        // if needs to be estimated per time range / validity period

        // if several value intervals
        } else {
            final Span<Double> valueSpan = valueSpanMap.getSpan(date);
            previousValue = valueSpan.getData();
            referenceDateSpan = valueSpan.getStart();
            // if the Span considered is from past infinity to valueSpanEndDate it is
            // impossible to addValidAfter past infinity because it is creating a new span that
            // is why the below trick was set up
            if (referenceDateSpan.equals(AbsoluteDate.PAST_INFINITY)) {
                referenceDateSpan = valueSpan.getEnd();
                this.valueSpanMap.addValidBefore(FastMath.max(minValue, FastMath.min(maxValue, newValue)),
                                                 referenceDateSpan, false);
            } else {
                this.valueSpanMap.addValidAfter(FastMath.max(minValue, FastMath.min(maxValue, newValue)),
                                                referenceDateSpan, false);
            }
        }

        for (final ParameterObserver observer : observers) {
            observer.valueChanged(previousValue, this, date);
        }
    }


    /** Set parameter value. Only usable on ParameterDriver
     * which have only 1 span on their TimeSpanMap value (that is
     * to say for which the setPeriod method wasn't called)
     * <p>
     * If {@code newValue} is below {@link #getMinValue()}, it will
     * be silently set to {@link #getMinValue()}. If {@code newValue} is
     * above {@link #getMaxValue()}, it will be silently set to {@link
     * #getMaxValue()}.
     * </p>
     * @param newValue new value to set
     */
    public void setValue(final double newValue) {
        if (getNbOfValues() == 1) {
            final AbsoluteDate referenceDateSpan = AbsoluteDate.ARBITRARY_EPOCH;
            final double previousValue = this.getValue(referenceDateSpan);
            this.valueSpanMap = new TimeSpanMap<>(FastMath.max(minValue, FastMath.min(maxValue, newValue)));
            for (final ParameterObserver observer : observers) {
                observer.valueChanged(previousValue, this, referenceDateSpan);
            }
        } else {
            throw new OrekitIllegalStateException(OrekitMessages.PARAMETER_WITH_SEVERAL_ESTIMATED_VALUES, name, "setValue(date)");
        }
    }

    /** Configure a parameter selection status.
     * <p>
     * Selection is used for estimated parameters in orbit determination,
     * or to compute the Jacobian matrix in partial derivatives computation.
     * </p>
     * @param selected if true the parameter is selected,
     * otherwise it will be fixed
     */
    public void setSelected(final boolean selected) {
        final boolean previousSelection = isSelected();
        this.selected = selected;
        for (final ParameterObserver observer : observers) {
            observer.selectionChanged(previousSelection, this);
        }
    }

    /** Check if parameter is selected.
     * <p>
     * Selection is used for estimated parameters in orbit determination,
     * or to compute the Jacobian matrix in partial derivatives computation.
     * </p>
     * @return true if parameter is selected, false if it is not
     */
    public boolean isSelected() {
        return selected;
    }

    /** Set parameter estimation to continuous, by default step estimation.
     * <p> Continuous estimation : when a value wants to be known at date
     * t, the value returned will be an interpolation between start value
     * of the span corresponding to date t and end value (which corresponds
     * to the start of the next span).
     * </p>
     * <p> Step estimation : when a value wants to be
     * known at date t, the value returned will be the value of the beginning
     * of span corresponding to date t, step estimation.
     * </p>
     * @param continuous if true the parameter will be estimated
     * with continuous estimation, if false with step estimation.
     */
    public void setContinuousEstimation(final boolean continuous) {
        final boolean previousEstimation = isContinuousEstimation();
        this.isEstimationContinuous = continuous;
        for (final ParameterObserver observer : observers) {
            observer.estimationTypeChanged(previousEstimation, this);
        }
    }

    /** Check if parameter estimation is continuous, that is to say when
     * a value wants to be known at date t, the value returned
     * will be an interpolation between start value on span corresponding
     * for date t and end value (which corresponds to the start of the next
     * span), continuous estimation. Or not continuous, that is to say when a value wants to be
     * known at date t, the value returned will be the value of the start
     * of span corresponding to date t, step estimation.
     * @return true if continuous estimation/definition, false if step estimation/definition
     * @since 12.0
     */
    public boolean isContinuousEstimation() {
        return isEstimationContinuous;
    }

    /** Get a text representation of the parameter.
     * @return text representation of the parameter, in the form name = value.
     */
    public String toString() {
        return name + " = " + valueSpanMap.get(AbsoluteDate.ARBITRARY_EPOCH);
    }

}
