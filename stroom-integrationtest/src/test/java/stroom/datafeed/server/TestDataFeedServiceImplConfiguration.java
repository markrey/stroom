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

package stroom.datafeed.server;

import stroom.util.logging.StroomLogger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import stroom.streamstore.server.MockStreamStore;
import stroom.util.thread.ThreadLocalBuffer;
import stroom.util.zip.HeaderMap;
import stroom.util.zip.HeaderMapFactory;

/**
 * @Configuration specific to TesetDataFeedServiceImpl.
 *
 *                The combination of mock and prod classes means this test needs
 *                its own context.
 */
@Configuration
public class TestDataFeedServiceImplConfiguration {
    private static final StroomLogger LOGGER = StroomLogger.getLogger(TestDataFeedServiceImplConfiguration.class);

    public TestDataFeedServiceImplConfiguration() {
        LOGGER.info("TestDataFeedServiceImplConfiguration loading...");
    }

    @Bean
    @Scope("request")
    public ThreadLocalBuffer requestThreadLocalBuffer() {
        return new ThreadLocalBuffer();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public MockHeaderMapFactory headerMapFactory() {
        return new MockHeaderMapFactory();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public HeaderMap headerMap(HeaderMapFactory headerMapFactory) {
        return headerMapFactory.create();
    }

    @Bean
    public MockStreamStore mockStreamStore() {
        return new MockStreamStore();
    }
}
