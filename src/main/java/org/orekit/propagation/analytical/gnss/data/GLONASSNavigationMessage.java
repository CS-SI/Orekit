/* Copyright 2002-2021 CS GROUP
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

/**
 * Container for data contained in a Glonass navigation message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class GLONASSNavigationMessage extends AbstractEphemerisMessage implements GLONASSOrbitalElements {

    /** Message frame time. */
    private double time;

    /** SV clock bias. */
    private double tauN;

    /** SV relative frequency bias. */
    private double gammaN;

    /** Frequency number. */
    private int frequencyNumber;

    /** Constructor. */
    public GLONASSNavigationMessage() {
        // Nothing to do ...
    }

    /** {@inheritDoc} */
    @Override
    public double getTN() {
        return tauN;
    }

    /**
     * Setter for the SV clock bias.
     * @param tn the SV clock bias
     */
    public void setTauN(final double tn) {
        this.tauN = tn;
    }

    /** {@inheritDoc} */
    @Override
    public double getGammaN() {
        return gammaN;
    }

    /**
     * Setter for the SV relative frequency bias.
     * @param gammaN the SV relative frequency bias.
     */
    public void setGammaN(final double gammaN) {
        this.gammaN = gammaN;
    }

    /**
     * Getter for the frequency number.
     * @return the frequency number
     */
    public int getFrequencyNumber() {
        return frequencyNumber;
    }

    /**
     * Setter for the frequency number.
     * @param frequencyNumber the number to set
     */
    public void setFrequencyNumber(final double frequencyNumber) {
        this.frequencyNumber = (int) frequencyNumber;
    }

    /** {@inheritDoc} */
    @Override
    public double getTime() {
        return time;
    }

    /**
     * Setter for the message frame time.
     * @param time the time to set
     */
    public void setTime(final double time) {
        this.time = time;
    }

}
