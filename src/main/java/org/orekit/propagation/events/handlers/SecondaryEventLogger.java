/* Copyright 2022-2025 Romain Serra.
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
package org.orekit.propagation.events.handlers;

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.Pair;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

import java.util.ArrayList;
import java.util.List;


/**
 * Event handler logging primary and secondary positional information.
 * @author Romain Serra
 *
 * @since 13.1
 */
public class SecondaryEventLogger implements EventHandler {

    /** Action to return. */
    private final Action action;

    /** Secondary. */
    private final PVCoordinatesProvider secondaryPVCoordinatesProvider;

    /** Logs for primary and secondary at events (as well as initial and terminal dates). */
    private final List<Pair<AbsolutePVCoordinates, PVCoordinates>> logs = new ArrayList<>();

    /**
     * Constructor.
     * @param secondaryPVCoordinatesProvider provider for secondary trajectory
     * @param action action to return when event occurs
     */
    public SecondaryEventLogger(final PVCoordinatesProvider secondaryPVCoordinatesProvider, final Action action) {
        this.secondaryPVCoordinatesProvider = secondaryPVCoordinatesProvider;
        this.action = action;
    }

    /**
     * Constructor with default Action (CONTINUE).
     * @param secondaryPVCoordinatesProvider provider for secondary trajectory
     */
    public SecondaryEventLogger(final PVCoordinatesProvider secondaryPVCoordinatesProvider) {
        this(secondaryPVCoordinatesProvider, Action.CONTINUE);
    }

    /**
     * Method to clear logs.
     */
    public void clearLogs() {
        logs.clear();
    }

    /**
     * Method to copy logs.
     * @return copy of logs
     */
    public List<Pair<AbsolutePVCoordinates, PVCoordinates>> copyLogs() {
        return new ArrayList<>(logs);
    }

    /**
     * Getter for the secondary trajectory provider.
     * @return secondary
     */
    public PVCoordinatesProvider getSecondaryPVCoordinatesProvider() {
        return secondaryPVCoordinatesProvider;
    }

    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target, final EventDetector detector) {
        EventHandler.super.init(initialState, target, detector);
        logs.add(extractLog(initialState));
    }

    @Override
    public void finish(final SpacecraftState finalState, final EventDetector detector) {
        EventHandler.super.finish(finalState, detector);
        logs.add(extractLog(finalState));
    }

    @Override
    public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {
        logs.add(extractLog(s));
        return action;
    }

    /**
     * Extract log from primary's state.
     * @param s primary
     * @return pair of primary and secondary positional information
     */
    private Pair<AbsolutePVCoordinates, PVCoordinates> extractLog(final SpacecraftState s) {
        final AbsoluteDate date = s.getDate();
        final Frame frame = s.getFrame();
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, date, s.getPVCoordinates());
        final PVCoordinates pvCoordinates = secondaryPVCoordinatesProvider.getPVCoordinates(date, frame);
        return new Pair<>(absolutePVCoordinates, pvCoordinates);
    }
}
