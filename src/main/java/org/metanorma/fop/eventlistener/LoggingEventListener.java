package org.metanorma.fop.eventlistener;

import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;
import org.metanorma.utils.LoggerHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingEventListener implements EventListener {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private boolean skipFatal;

    private final Set<String> loggedMessages = new HashSet<String>();

    /** {@inheritDoc} */
    public void processEvent(Event event) {
        String msg = EventFormatter.format(event);
        EventSeverity severity = event.getSeverity();
        if (severity == EventSeverity.INFO) {
            logger.info(msg);
        } else if (severity == EventSeverity.WARN) {
            // we want to prevent logging of duplicate messages in situations where they are likely
            // to occur; for instance, warning related to layout do not repeat (since line number
            // will be different) and as such we do not try to filter them here; on the other hand,
            // font related warnings are very likely to repeat and we try to filter them out here;
            // the same may happen with missing images (but not implemented yet).
            String eventGroupID = event.getEventGroupID();
            if (eventGroupID.equals("org.apache.fop.fonts.FontEventProducer")) {
                if (!loggedMessages.contains(msg)) {
                    loggedMessages.add(msg);
                    logger.warning(msg);
                }
            } else if (msg.contains("\"empty_bookmark\"")) {
                // skip message, no need output 'Bookmarks: Unresolved ID reference "empty_bookmark" found.'
            } else {
                logger.warning(msg);
            }
        } else if (severity == EventSeverity.ERROR) {
            if (event.getParam("e") != null) {
                logger.log(Level.SEVERE, msg, (Throwable)event.getParam("e"));
            } else {
                logger.severe(msg);
            }
        } else if (severity == EventSeverity.FATAL) {
            if (!skipFatal) {
                if (event.getParam("e") != null) {
                    logger.log(Level.SEVERE, msg, (Throwable)event.getParam("e"));
                } else {
                    logger.severe(msg);
                }
            }
        } else {
            assert false;
        }
    }
}
