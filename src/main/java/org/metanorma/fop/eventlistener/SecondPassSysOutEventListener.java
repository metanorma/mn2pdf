package org.metanorma.fop.eventlistener;

import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.model.EventSeverity;
import org.metanorma.utils.LoggerHelper;

import java.util.logging.Level;
import java.util.logging.Logger;

/** A simple event listener that writes the events to stdout and sterr. */
public class SecondPassSysOutEventListener implements org.apache.fop.events.EventListener {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    /** {@inheritDoc} */
    public void processEvent(Event event) {
        String msg = EventFormatter.format(event);
        EventSeverity severity = event.getSeverity();
        if (severity == EventSeverity.INFO) {
            if(msg.startsWith("Rendered page #")) {
                //System.out.println("[INFO] Intermediate format. " + msg);
                logger.log(Level.INFO, "[INFO] Intermediate format. {0}", msg);
            }
        } else if (severity == EventSeverity.WARN) {
            //System.out.println("[WARN] " + msg);
        } else if (severity == EventSeverity.ERROR) {
            //System.err.println("[ERROR] " + msg);
            logger.log(Level.SEVERE, "[ERROR] {0}", msg);
        } else if (severity == EventSeverity.FATAL) {
            //System.err.println("[FATAL] " + msg);
            logger.log(Level.SEVERE, "[FATAL] {0}", msg);
        } else {
            assert false;
        }
    }
}