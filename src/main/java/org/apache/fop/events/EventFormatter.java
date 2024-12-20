/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: EventFormatter.java 1734671 2016-03-12 05:39:53Z gadams $ */

package org.apache.fop.events;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.fop.events.model.EventSeverity;
import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.utils.XMLResourceBundle;
import org.apache.fop.utils.text.AdvancedMessageFormat;
import org.apache.fop.utils.text.AdvancedMessageFormat.Part;
import org.apache.fop.utils.text.AdvancedMessageFormat.PartFactory;

/**
 * Converts events into human-readable, localized messages.
 */
public final class EventFormatter {

    private static final Pattern INCLUDES_PATTERN = Pattern.compile("\\{\\{.+\\}\\}");

    private static Log log = LogFactory.getLog(EventFormatter.class);

    private EventFormatter() {
        //utility class
    }

    private static ResourceBundle getBundle(String groupID, Locale locale) {
        ResourceBundle bundle;
        String baseName = (groupID != null) ? groupID : EventFormatter.class.getName();
        try {
            ClassLoader classLoader = EventFormatter.class.getClassLoader();
            bundle = XMLResourceBundle.getXMLBundle(baseName, locale, classLoader);
        } catch (MissingResourceException e) {
            if (log.isTraceEnabled()) {
                log.trace("No XMLResourceBundle for " + baseName + " available.");
            }
            bundle = null;
        }
        return bundle;
    }

    /**
     * Formats an event using the default locale.
     * @param event the event
     * @return the formatted message
     */
    public static String format(Event event) {
        return format(event, event.getLocale());
    }

    /**
     * Formats an event using a given locale.
     * @param event the event
     * @param locale the locale
     * @return the formatted message
     */
    public static String format(Event event, Locale locale) {
        return format(event, getBundle(event.getEventGroupID(), locale));
    }

    private static String format(Event event, ResourceBundle bundle) {
        assert event != null;
        String key = event.getEventKey();
        String template;
        if (bundle != null) {
            String elementName = (String)event.getParams().get("elementName");
            if (elementName != null) {
                String className = event.getSource().getClass().toString();
                try {
                    String elementId = ((LayoutManager) (event.getSource())).getFObj().getId();
                    if (elementId != null && elementId.startsWith("__internal_layout__")) { //
                        // special case when element (for instance, table) is using for block position on the page, no need warning
                        return "";
                    }
                } catch (Exception ex) { }
            }
            if (key.equals("overconstrainedAdjustEndIndent") && elementName != null && elementName.equals("fo:table")) {
                key = "overconstrainedAdjustEndIndentTable";
                event.setSeverity(EventSeverity.WARN);
            }
            template = bundle.getString(key);
        } else {
            template = "Missing bundle. Can't lookup event key: '" + key + "'.";
        }
        return format(event, processIncludes(template, bundle));
    }

    private static String processIncludes(String template, ResourceBundle bundle) {
        CharSequence input = template;
        int replacements;
        StringBuffer sb;
        do {
            sb = new StringBuffer(Math.max(16, input.length()));
            replacements = processIncludesInner(input, sb, bundle);
            input = sb;
        } while (replacements > 0);
        String s = sb.toString();
        return s;
    }

    private static int processIncludesInner(CharSequence template, StringBuffer sb,
            ResourceBundle bundle) {
        int replacements = 0;
        if (bundle != null) {
            Matcher m = INCLUDES_PATTERN.matcher(template);
            while (m.find()) {
                String include = m.group();
                include = include.substring(2, include.length() - 2);
                m.appendReplacement(sb, bundle.getString(include));
                replacements++;
            }
            m.appendTail(sb);
        }
        return replacements;
    }

    /**
     * Formats the event using a given pattern. The pattern needs to be compatible with
     * {@link AdvancedMessageFormat}.
     * @param event the event
     * @param pattern the pattern (compatible with {@link AdvancedMessageFormat})
     * @return the formatted message
     */
    public static String format(Event event, String pattern) {
        AdvancedMessageFormat format = new AdvancedMessageFormat(pattern);
        Map params = new java.util.HashMap(event.getParams());

        String elementId = (String) params.get("elementId");
        if (elementId == null && params.get("elementName") != null) {
            elementId = "";
            String elementPage = "unknown";
            Object source = event.getSource();
            LayoutManager lm = (source instanceof LayoutManager) ? (LayoutManager)source : null;

            String id = "";
            String page = "";

            if (lm != null) {
                try {
                    id = lm.getFObj().getId();
                    page = lm.getPSLM().getCurrentPage().getPageViewport().getPageNumberString();
                } catch (Exception ex) { }
            }

            if (id != null && !id.isEmpty()) {
                elementId = "(id='" + id + "')";
            }

            if (page == null || page.isEmpty()) {
                // try to get the parameter 'page'
                page = (String) params.get("page");
            }

            if (page != null && !page.isEmpty()) {
                elementPage = page;
            }

            params.put("elementId",  elementId);
            params.put("page",  elementPage);
        }

        params.put("source", event.getSource());
        params.put("severity", event.getSeverity());
        params.put("groupID", event.getEventGroupID());
        params.put("locale", event.getLocale());
        return format.format(params);
    }

    private static class LookupFieldPart implements Part {

        private String fieldName;

        public LookupFieldPart(String fieldName) {
            this.fieldName = fieldName;
        }

        public boolean isGenerated(Map params) {
            return getKey(params) != null;
        }

        public void write(StringBuffer sb, Map params) {
            String groupID = (String) params.get("groupID");
            Locale locale = (Locale) params.get("locale");
            ResourceBundle bundle = getBundle(groupID, locale);
            if (bundle != null) {
                sb.append(bundle.getString(getKey(params)));
            }
        }

        private String getKey(Map params) {
            return (String)params.get(fieldName);
        }

        /** {@inheritDoc} */
        public String toString() {
            return "{" + this.fieldName + ", lookup}";
        }

    }

    /** PartFactory for lookups. */
    public static class LookupFieldPartFactory implements PartFactory {

        /** {@inheritDoc} */
        public Part newPart(String fieldName, String values) {
            return new LookupFieldPart(fieldName);
        }

        /** {@inheritDoc} */
        public String getFormat() {
            return "lookup";
        }

    }

}
