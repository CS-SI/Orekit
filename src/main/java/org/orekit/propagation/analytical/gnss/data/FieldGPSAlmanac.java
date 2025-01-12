/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.propagation.analytical.gnss.data;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;

import java.util.function.Function;

/**
 * This class holds a GPS almanac as read from SEM or YUMA files.
 *
 * <p>Depending on the source (SEM or YUMA), some fields may be filled in or not.
 * An almanac read from a YUMA file doesn't hold SVN number, average URA and satellite
 * configuration.</p>
 *
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 *
 */
public class FieldGPSAlmanac<T extends CalculusFieldElement<T>>
    extends FieldAbstractAlmanac<T, GPSAlmanac> {

    /** Source of the almanac. */
    private String src;

    /** SVN number. */
    private int svn;

    /** Health status. */
    private int health;

    /** Average URA. */
    private int ura;

    /** Satellite configuration. */
    private int config;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldGPSAlmanac(final Field<T> field, final GPSAlmanac original) {
        super(field, original);
        setSource(original.getSource());
        setSVN(original.getSVN());
        setHealth(original.getHealth());
        setURA(original.getURA());
        setSatConfiguration(original.getSatConfiguration());
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldGPSAlmanac(final Function<V, T> converter,
                                                               final FieldGPSAlmanac<V> original) {
        super(converter, original);
        setSource(original.getSource());
        setSVN(original.getSVN());
        setHealth(original.getHealth());
        setURA(original.getURA());
        setSatConfiguration(original.getSatConfiguration());
    }

    /** {@inheritDoc} */
    @Override
    public GPSAlmanac toNonField() {
        return new GPSAlmanac(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, G extends FieldGnssOrbitalElements<U, GPSAlmanac>>
        G changeField(final Function<T, U> converter) {
        return (G) new FieldGPSAlmanac<>(converter, this);
    }

    /**
     * Setter for the Square Root of Semi-Major Axis (m^1/2).
     * <p>
     * In addition, this method set the value of the Semi-Major Axis.
     * </p>
     * @param sqrtA the Square Root of Semi-Major Axis (m^1/2)
     */
    public void setSqrtA(final T sqrtA) {
        setSma(sqrtA.square());
    }

    /**
     * Gets the source of this GPS almanac.
     * <p>Sources can be SEM or YUMA, when the almanac is read from a file.</p>
     *
     * @return the source of this GPS almanac
     */
    public String getSource() {
        return src;
    }

    /**
     * Sets the source of this GPS almanac.
     *
     * @param source the source of this GPS almanac
     */
    public void setSource(final String source) {
        this.src = source;
    }

    /**
     * Gets the satellite "SVN" reference number.
     *
     * @return the satellite "SVN" reference number
     */
    public int getSVN() {
        return svn;
    }

    /**
     * Sets the "SVN" reference number.
     *
     * @param svnNumber the number to set
     */
    public void setSVN(final int svnNumber) {
        this.svn = svnNumber;
    }

    /**
     * Gets the Health status.
     *
     * @return the Health status
     */
    public int getHealth() {
        return health;
    }

    /**
     * Sets the health status.
     *
     * @param health the health status to set
     */
    public void setHealth(final int health) {
        this.health = health;
    }

    /**
     * Gets the average URA number.
     *
     * @return the average URA number
     */
    public int getURA() {
        return ura;
    }

    /**
     * Sets the average URA number.
     *
     * @param uraNumber the URA number to set
     */
    public void setURA(final int uraNumber) {
        this.ura = uraNumber;
    }

    /**
     * Gets the satellite configuration.
     *
     * @return the satellite configuration
     */
    public int getSatConfiguration() {
        return config;
    }

    /**
     * Sets the satellite configuration.
     *
     * @param satConfiguration the satellite configuration to set
     */
    public void setSatConfiguration(final int satConfiguration) {
        this.config = satConfiguration;
    }

}
