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

package stroom.streamtask.server;

import java.util.List;

import javax.annotation.Resource;

import stroom.util.config.StroomProperties;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import stroom.AbstractCoreIntegrationTest;
import stroom.CommonTestControl;
import stroom.CommonTestScenarioCreator;
import stroom.feed.shared.Feed;
import stroom.feed.shared.FeedService;
import stroom.jobsystem.server.MockTask;
import stroom.node.server.NodeCache;
import stroom.node.shared.FindNodeCriteria;
import stroom.node.shared.Node;
import stroom.node.shared.NodeService;
import stroom.streamstore.server.FindStreamVolumeCriteria;
import stroom.streamstore.server.StreamDeleteExecutor;
import stroom.streamstore.server.StreamMaintenanceService;
import stroom.streamstore.server.StreamRetentionExecutor;
import stroom.streamstore.server.StreamStore;
import stroom.streamstore.server.StreamTarget;
import stroom.streamstore.shared.Stream;
import stroom.streamstore.shared.StreamType;
import stroom.streamstore.shared.StreamVolume;
import stroom.task.server.TaskManager;
import stroom.task.server.TaskMonitorImpl;
import stroom.volume.server.VolumeServiceImpl;

/**
 * Test the archiving stuff.
 *
 * Create some old files and make sure they get archived.
 */
public class TestStreamArchiveTask extends AbstractCoreIntegrationTest {
    private static final int HIGHER_REPLICATION_COUNT = 2;
    private static final int SIXTY = 60;
    private static final int FIFTY_FIVE = 55;
    private static final int FIFTY = 50;

    @Resource
    private StreamStore streamStore;
    @Resource
    private StreamMaintenanceService streamMaintenanceService;
    @Resource
    private FeedService feedService;
    @Resource
    private FileSystemCleanExecutor fileSystemCleanTaskExecutor;
    @Resource
    private TaskManager taskManager;
    @Resource
    private CommonTestScenarioCreator commonTestScenarioCreator;
    @Resource
    private CommonTestControl commonTestControl;
    @Resource
    private NodeCache nodeCache;
    @Resource
    private NodeService nodeService;
    @Resource
    private StreamTaskCreatorImpl streamTaskCreator;
    @Resource
    private StreamRetentionExecutor streamRetentionExecutor;
    @Resource
    private StreamDeleteExecutor streamDeleteExecutor;

    private int initialReplicationCount = 1;

    @Override
    protected void onBefore() {
        initialReplicationCount = StroomProperties.getIntProperty(VolumeServiceImpl.PROP_RESILIENT_REPLICATION_COUNT, 1);
        StroomProperties.setIntProperty(VolumeServiceImpl.PROP_RESILIENT_REPLICATION_COUNT, HIGHER_REPLICATION_COUNT,
                StroomProperties.Source.TEST);
    }

    @Override
    protected void onAfter() {
        StroomProperties.setIntProperty(VolumeServiceImpl.PROP_RESILIENT_REPLICATION_COUNT, initialReplicationCount,
                StroomProperties.Source.TEST);
    }

    @Test
    public void testCheckArchive() throws Exception {
        nodeCache.getDefaultNode();
        final List<Node> nodeList = nodeService.find(new FindNodeCriteria());
        for (final Node node : nodeList) {
            fileSystemCleanTaskExecutor.clean(new MockTask("Test"), node.getId());
        }

        final DateTime oldDate = new DateTime().minusDays(SIXTY);
        final DateTime newDate = new DateTime().minusDays(FIFTY);

        // Write a file 2 files ... on we leave locked and the other not locked
        Feed feed = commonTestScenarioCreator.createSimpleFeed();
        feed.setRetentionDayAge(FIFTY_FIVE);
        feed = feedService.save(feed);

        final Stream oldFile = Stream.createStreamForTesting(StreamType.RAW_EVENTS, feed, null,
                oldDate.toDate().getTime());
        final Stream newFile = Stream.createStreamForTesting(StreamType.RAW_EVENTS, feed, null,
                newDate.toDate().getTime());

        final StreamTarget oldFileTarget = streamStore.openStreamTarget(oldFile);
        oldFileTarget.getOutputStream().write("MyTest".getBytes());
        streamStore.closeStreamTarget(oldFileTarget);

        final StreamTarget newFileTarget = streamStore.openStreamTarget(newFile);
        newFileTarget.getOutputStream().write("MyTest".getBytes());
        streamStore.closeStreamTarget(newFileTarget);

        // Now we have added some data create some associated stream tasks.
        // TODO : At some point we need to change deletion to delete streams and
        // not tasks. Tasks should be deleted if an associated source stream is
        // deleted if they exist, however currently streams are only deleted if
        // their associated task exists which would prevent us from deleting
        // streams that have no task associated with them.
        streamTaskCreator.createTasks(new TaskMonitorImpl());

        List<StreamVolume> oldVolumeList = streamMaintenanceService
                .find(FindStreamVolumeCriteria.create(oldFileTarget.getStream()));
        Assert.assertEquals("Expecting 2 stream volumes", HIGHER_REPLICATION_COUNT, oldVolumeList.size());

        List<StreamVolume> newVolumeList = streamMaintenanceService
                .find(FindStreamVolumeCriteria.create(newFileTarget.getStream()));
        Assert.assertEquals("Expecting 2 stream volumes", HIGHER_REPLICATION_COUNT, newVolumeList.size());

        streamRetentionExecutor.exec();
        streamDeleteExecutor.delete(System.currentTimeMillis());

        // Test Again
        oldVolumeList = streamMaintenanceService.find(FindStreamVolumeCriteria.create(oldFileTarget.getStream()));
        Assert.assertEquals("Expecting 0 stream volumes", 0, oldVolumeList.size());

        newVolumeList = streamMaintenanceService.find(FindStreamVolumeCriteria.create(newFileTarget.getStream()));
        Assert.assertEquals("Expecting 2 stream volumes", HIGHER_REPLICATION_COUNT, newVolumeList.size());

        // Test they are
        oldVolumeList = streamMaintenanceService.find(FindStreamVolumeCriteria.create(oldFileTarget.getStream()));
        Assert.assertEquals("Expecting 0 stream volumes", 0, oldVolumeList.size());
        newVolumeList = streamMaintenanceService.find(FindStreamVolumeCriteria.create(newFileTarget.getStream()));
        Assert.assertEquals("Expecting 2 stream volumes", HIGHER_REPLICATION_COUNT, newVolumeList.size());
    }
}
