/* Copyright 2019 CS Systèmes d'Information
 * All rights reserved.
 */
package org.orekit.gnss.metric.ntrip;

import org.orekit.gnss.metric.messages.ParsedMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/** {@link RTCMMessageObserver Message observer} that simply logs messages counts.
 * @author Luc Maisonobe
 */
public class LoggingObserver implements MessageObserver {

    /** Count by message type. */
    private final Map<Integer, Integer> typesCounts;

    /** Count by mount point. */
    private final Map<String, Integer> mountPointsCounts;

    /** Simple constructor.
     */
    public LoggingObserver() {
        typesCounts       = new HashMap<>();
        mountPointsCounts = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public void messageAvailable(final String mountPoint, final ParsedMessage message) {

        // log per message type
        final Integer tc = typesCounts.get(message.getTypeCode());
        if (tc == null) {
            typesCounts.put(message.getTypeCode(), 1);
        } else {
            typesCounts.put(message.getTypeCode(), tc + 1);
        }

        // log per mount point
        final Integer mpc = mountPointsCounts.get(mountPoint);
        if (mpc == null) {
            mountPointsCounts.put(mountPoint, 1);
        } else {
            mountPointsCounts.put(mountPoint, mpc + 1);
        }

    }

    /** Get the message count by message type.
     * @return read-only view of a message type → count map
     */
    public Map<Integer, Integer> getCountByMessageType() {
        return Collections.unmodifiableMap(typesCounts);
    }

    /** Get the message count by mount point.
     * @return read-only view of a mount point → count map
     */
    public Map<String, Integer> getCountByMountPoint() {
        return Collections.unmodifiableMap(mountPointsCounts);
    }

}
