/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.pipeline.server.filter;

import javax.annotation.Resource;

import stroom.util.logging.StroomLogger;
import stroom.util.spring.StroomScope;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import stroom.node.shared.RecordCountService;
import stroom.node.shared.RecordCounter;
import stroom.pipeline.server.factory.ConfigurableElement;
import stroom.pipeline.server.factory.ElementIcons;
import stroom.pipeline.server.factory.PipelineProperty;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.pipeline.state.RecordCount;

/**
 * A SAX filter used to count the number of first level elements in an XML
 * instance. The first level elements are assumed to be records in the context
 * of event processing.
 */
@Component
@Scope(StroomScope.PROTOTYPE)
@ConfigurableElement(type = "RecordCountFilter", category = Category.FILTER, roles = { PipelineElementType.ROLE_TARGET,
        PipelineElementType.ROLE_HAS_TARGETS }, icon = ElementIcons.RECORD_COUNT)
public class RecordCountFilter extends AbstractXMLFilter implements RecordCounter {
    private static final StroomLogger LOGGER = StroomLogger.getLogger(RecordCountFilter.class);
    private static final int LOG_COUNT = 10000;

    private boolean countRead = true;
    private int depth = 0;
    private long count = 0;
    private long lastSnapshot;
    private long startMs;

    private RecordCount recordCount;

    @Resource
    private RecordCountService recordCountService;

    /**
     * @throws SAXException
     *             Not thrown.
     * @see stroom.pipeline.server.filter.AbstractXMLFilter#startProcessing()
     */
    @Override
    public void startProcessing() {
        try {
            startMs = System.currentTimeMillis();
            if (recordCountService != null) {
                if (countRead) {
                    recordCountService.addRecordReadCounter(this);
                } else {
                    recordCountService.addRecordWrittenCounter(this);
                }
            }
        } finally {
            super.startProcessing();
        }
    }

    /**
     * @throws SAXException
     *             Not thrown.
     * @see stroom.pipeline.server.filter.AbstractXMLFilter#endProcessing()
     */
    @Override
    public void endProcessing() {
        try {
            if (recordCountService != null) {
                if (countRead) {
                    recordCountService.removeRecordReadCounter(this);
                } else {
                    recordCountService.removeRecordWrittenCounter(this);
                }
            }
        } finally {
            super.endProcessing();
        }
    }

    /**
     * This method tells filters that a stream is about to be parsed so that
     * they can complete any setup necessary.
     *
     * @throws SAXException
     *             Could be thrown by an implementing class.
     */
    @Override
    public void startStream() {
        try {
            depth = 0;
        } finally {
            super.startStream();
        }
    }

    /**
     * This method tells filters that a stream has finished parsing so cleanup
     * can be performed.
     *
     * @throws SAXException
     *             Could be thrown by an implementing class.
     */
    @Override
    public void endStream() {
        try {
            super.endStream();
        } finally {
            recordCount.setDuration(System.currentTimeMillis() - startMs);
            if (countRead) {
                recordCount.setRead(count);
            } else {
                recordCount.setWritten(count);
            }
            depth = 0;
        }
    }

    /**
     * Fired on start element.
     *
     * @param uri
     *            the Namespace URI, or the empty string if the element has no
     *            Namespace URI or if Namespace processing is not being
     *            performed
     * @param localName
     *            the local name (without prefix), or the empty string if
     *            Namespace processing is not being performed
     * @param qName
     *            the qualified name (with prefix), or the empty string if
     *            qualified names are not available
     * @param atts
     *            the attributes attached to the element. If there are no
     *            attributes, it shall be an empty Attributes object. The value
     *            of this object after startElement returns is undefined
     * @throws org.xml.sax.SAXException
     *             any SAX exception, possibly wrapping another exception
     * @see #endElement
     * @see org.xml.sax.Attributes
     * @see org.xml.sax.helpers.AttributesImpl
     */
    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes atts)
            throws SAXException {
        super.startElement(uri, localName, qName, atts);

        depth++;

        if (depth == 2) {
            // This is a first level element.
            count++;

            if (LOGGER.isDebugEnabled()) {
                if (count % LOG_COUNT == 0) {
                    if (countRead) {
                        LOGGER.debug("Records read = " + count);
                    } else {
                        LOGGER.debug("Records written = " + count);
                    }
                }
            }
        }
    }

    /**
     * Fired on an end element.
     *
     * @param uri
     *            the Namespace URI, or the empty string if the element has no
     *            Namespace URI or if Namespace processing is not being
     *            performed
     * @param localName
     *            the local name (without prefix), or the empty string if
     *            Namespace processing is not being performed
     * @param qName
     *            the qualified XML name (with prefix), or the empty string if
     *            qualified names are not available
     * @throws org.xml.sax.SAXException
     *             any SAX exception, possibly wrapping another exception
     */
    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        depth--;
    }

    public long getCount() {
        return count;
    }

    /**
     * @param countRead
     *            Sets whether we are counting records read or records written.
     */
    @PipelineProperty(description = "Is this filter counting records read or records written?", defaultValue = "true")
    public void setCountRead(final boolean countRead) {
        this.countRead = countRead;
    }

    @Override
    public long getAndResetCount() {
        final long snapshot = count;
        final long delta = snapshot - lastSnapshot;
        lastSnapshot = snapshot;
        return delta;
    }

    @Resource
    public void setRecordCount(final RecordCount recordCount) {
        this.recordCount = recordCount;
    }
}
