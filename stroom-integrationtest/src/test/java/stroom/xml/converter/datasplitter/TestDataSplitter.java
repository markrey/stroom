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

package stroom.xml.converter.datasplitter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import stroom.AbstractProcessIntegrationTest;
import stroom.feed.shared.Feed;
import stroom.pipeline.shared.TextConverter.TextConverterType;
import stroom.test.StroomProcessTestFileUtil;
import stroom.util.spring.StroomBeanStore;
import stroom.util.task.TaskScopeContextHolder;
import stroom.xml.F2XTestUtil;
import stroom.xml.XMLValidator;

// TODO : Add test data
@Ignore("Add test data")
public class TestDataSplitter extends AbstractProcessIntegrationTest {
    @Resource
    private StroomBeanStore beanStore;

    /**
     * Tests a basic CSV file.
     *
     * @throws Exception
     *             Might be thrown while performing the test.
     */
    @Test
    public void testBasicCSV() throws Exception {
        final String xml = runF2XTest(TextConverterType.DATA_SPLITTER, "TestDataSplitter/CSVWithHeading.ds",
                new ByteArrayInputStream("h1,h2\ntoken1a,token1b\ntoken2a,token2b\n".getBytes()));

        final String example = "<?xml version=\"1.1\" encoding=\"UTF-8\"?>" + "<records " + "xmlns=\"records:2\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"records:2 file://records-v1.1.xsd\" " + "Version=\"1.1\">" + "<record>"
                + "<data name=\"h1\" value=\"token1a\"/>" + "<data name=\"h2\" value=\"token1b\"/>"
                + "</record><record>" + "<data name=\"h1\" value=\"token2a\"/>"
                + "<data name=\"h2\" value=\"token2b\"/>" + "</record></records>";

        Assert.assertEquals(example, xml);
    }

    /**
     * Tests a sample network monitoring CSV file.
     *
     * @throws Exception
     *             Might be thrown while performing the test.
     */
    @Test
    public void testNetworkMonitoringSample() throws Exception {
        final String xml = runF2XTest(TextConverterType.DATA_SPLITTER, "TestDataSplitter/CSVWithHeading.ds",
                StroomProcessTestFileUtil.getInputStream("TestDataSplitter/NetworkMonitoringSample.in"));

        Assert.assertTrue(xml.indexOf("<data name=\"IPAddress\" value=\"192.168.0.3\"/>") != -1);
    }

    /**
     * Tests a sample network monitoring CSV file and tries to transform it with
     * XSL.
     *
     * @throws Exception
     *             Might be thrown while performing the test.
     */
    @Test
    public void testNetworkMonitoringSampleWithXSL() throws Exception {
        runFullTest(new Feed("NetworkMonitoring-EVENTS"), TextConverterType.DATA_SPLITTER,
                "TestDataSplitter/SimpleCSVSplitter.ds", "TestDataSplitter/NetworkMonitoring.xsl",
                "TestDataSplitter/NetworkMonitoringSample.in", 0);
    }

    /**
     * Tests a sample network monitoring CSV file and tries to transform it with
     * XSL.
     *
     * @throws Exception
     *             Might be thrown while performing the test.
     */
    @Test
    public void testDS3NetworkMonitoringSampleWithXSL() throws Exception {
        runFullTest(new Feed("NetworkMonitoring-EVENTS"), TextConverterType.DATA_SPLITTER,
                "TestDataSplitter/CSVWithHeading.ds", "TestDataSplitter/DS3NetworkMonitoring.xsl",
                "TestDataSplitter/NetworkMonitoringSample.in", 0);
    }

    /**
     * First stage ref data change.
     *
     * @throws Exception
     *             NA
     */
    @Test
    public void testRefDataCSV() throws Exception {
        final String xml = runF2XTest(TextConverterType.DATA_SPLITTER, "TestDataSplitter/SimpleCSVSplitter.ds",
                StroomProcessTestFileUtil.getInputStream("TestDataSplitter/SampleRefData-HostNameToIP.in"));

        Assert.assertTrue(xml.indexOf("<data name=\"IP Address\" value=\"192.168.0.10\"/>") != -1);
    }

    /**
     * Tests a sample ref data CSV file and tries to transform it with XSL.
     *
     * @throws Exception
     *             Might be thrown while performing the test.
     */
    @Test
    public void testRefDataCSVWithXSL() throws Exception {
        final Feed refFeed = new Feed("HostNameToIP-REFERENCE");
        refFeed.setReference(true);
        runFullTest(refFeed, TextConverterType.DATA_SPLITTER, "TestDataSplitter/SimpleCSVSplitter.ds",
                "TestDataSplitter/SampleRefData-HostNameToIP.xsl", "TestDataSplitter/SampleRefData-HostNameToIP.in", 0);
    }

    private String runF2XTest(final TextConverterType textConverterType, final String textConverterLocation,
            final InputStream inputStream) {
        validate(textConverterType, textConverterLocation);

        final F2XTestUtil f2xTestUtil = beanStore.getBean(F2XTestUtil.class);
        final String xml = f2xTestUtil.runF2XTest(textConverterType, textConverterLocation, inputStream);
        return xml;
    }

    private String runFullTest(final Feed feed, final TextConverterType textConverterType,
            final String textConverterLocation, final String xsltLocation, final String dataLocation,
            final int expectedWarnings) {
        validate(textConverterType, textConverterLocation);

        try {
            TaskScopeContextHolder.addContext();
            final F2XTestUtil f2xTestUtil = beanStore.getBean(F2XTestUtil.class);
            final String xml = f2xTestUtil.runFullTest(feed, textConverterType, textConverterLocation, xsltLocation,
                    dataLocation, expectedWarnings);
            return xml;
        } finally {
            TaskScopeContextHolder.removeContext();
        }
    }

    private void validate(final TextConverterType textConverterType, final String textConverterLocation) {
        try {
            TaskScopeContextHolder.addContext();
            final XMLValidator xmlValidator = beanStore.getBean(XMLValidator.class);
            // Start by validating the resource.
            if (textConverterType == TextConverterType.DATA_SPLITTER) {
                final String message = xmlValidator.getInvalidXmlResourceMessage(textConverterLocation, true);
                Assert.assertTrue(message, message.length() == 0);
            }
        } finally {
            TaskScopeContextHolder.removeContext();
        }
    }
}
