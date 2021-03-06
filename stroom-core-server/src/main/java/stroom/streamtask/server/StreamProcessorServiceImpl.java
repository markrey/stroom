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

import stroom.entity.server.CriteriaLoggingUtil;
import stroom.entity.server.QueryAppender;
import stroom.entity.server.SystemEntityServiceImpl;
import stroom.entity.server.UserManagerQueryUtil;
import stroom.entity.server.util.StroomEntityManager;
import stroom.entity.server.util.SQLBuilder;
import stroom.entity.server.util.SQLUtil;
import stroom.pipeline.shared.PipelineEntity;
import stroom.security.Secured;
import stroom.streamtask.shared.FindStreamProcessorCriteria;
import stroom.streamtask.shared.StreamProcessor;
import stroom.streamtask.shared.StreamProcessorService;
import event.logging.BaseAdvancedQueryItem;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

@Transactional
@Component("streamProcessorService")
@Secured(StreamProcessor.MANAGE_PROCESSORS_PERMISSION)
public class StreamProcessorServiceImpl extends SystemEntityServiceImpl<StreamProcessor, FindStreamProcessorCriteria>
        implements StreamProcessorService {
    @Inject
    StreamProcessorServiceImpl(final StroomEntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public Class<StreamProcessor> getEntityClass() {
        return StreamProcessor.class;
    }

    @Override
    public FindStreamProcessorCriteria createCriteria() {
        return new FindStreamProcessorCriteria();
    }

    @Override
    public void appendCriteria(final List<BaseAdvancedQueryItem> items, final FindStreamProcessorCriteria criteria) {
        CriteriaLoggingUtil.appendEntityIdSet(items, "pipelineIdSet", criteria.getPipelineIdSet());
        CriteriaLoggingUtil.appendEntityIdSet(items, "folderIdSet", criteria.getFolderIdSet());
        super.appendCriteria(items, criteria);
    }

    @Override
    public StreamProcessorQueryAppender createQueryAppender(final StroomEntityManager entityManager) {
        return new StreamProcessorQueryAppender(entityManager);
    }

    private static class StreamProcessorQueryAppender extends QueryAppender<StreamProcessor, FindStreamProcessorCriteria> {
        public StreamProcessorQueryAppender(StroomEntityManager entityManager) {
            super(entityManager);
        }

        @Override
        protected void appendBasicJoin(final SQLBuilder sql, final String alias, final Set<String> fetchSet) {
            super.appendBasicJoin(sql, alias, fetchSet);
            if (fetchSet != null && fetchSet.contains(PipelineEntity.ENTITY_TYPE)) {
                sql.append(" LEFT	OUTER JOIN FETCH ");
                sql.append(alias);
                sql.append(".pipeline");
            }
        }

        @Override
        protected void appendBasicCriteria(final SQLBuilder sql, final String entityName,
                                           final FindStreamProcessorCriteria criteria) {
            super.appendBasicCriteria(sql, entityName, criteria);
            SQLUtil.appendSetQuery(sql, true, entityName + ".pipeline", criteria.getPipelineIdSet());

            UserManagerQueryUtil.appendFolderCriteria(criteria.getFolderIdSet(), entityName + ".pipeline.folder", sql, true,
                    getEntityManager());
        }
    }
}
