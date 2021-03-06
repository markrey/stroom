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

package stroom.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import stroom.security.SecurityContext;
import stroom.util.task.ServerTask;
import org.springframework.stereotype.Component;

import stroom.security.Insecure;
import stroom.entity.shared.FindFolderCriteria;
import stroom.importexport.server.ImportExportService;
import stroom.resource.server.ResourceStore;
import stroom.util.io.StreamUtil;
import stroom.util.shared.ResourceKey;

/**
 * B2B Export of the config
 */
@Component(ExportConfigServlet.BEAN_NAME)
public class ExportConfigServlet extends HttpServlet {
    public static final String BEAN_NAME = "exportConfigServlet";

    private static final long serialVersionUID = -4533441835216235920L;

    @Resource
    private transient SecurityContext securityContext;
    @Resource
    private transient ImportExportService importExportService;
    @Resource(name = "resourceStore")
    private transient ResourceStore resourceStore;

    /**
     * Method Interceptor needs to go on public API By-pass authentication /
     * authorisation checks This servlet is protected by a certifcate required
     * filter
     */
    @Override
    @Insecure
    public void service(final ServletRequest arg0, final ServletResponse arg1) throws ServletException, IOException {
        super.service(arg0, arg1);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        final ResourceKey tempResourceKey = resourceStore.createTempFile("StroomConfig.zip");

        securityContext.pushUser(ServerTask.INTERNAL_PROCESSING_USER);
        try {
            try {
                final File tempFile = resourceStore.getTempFile(tempResourceKey);

                final FindFolderCriteria criteria = new FindFolderCriteria();
                criteria.getFolderIdSet().setDeep(true);
                criteria.getFolderIdSet().setMatchNull(Boolean.TRUE);

                importExportService.exportConfig(criteria, tempFile, true, new ArrayList<String>());

                StreamUtil.streamToStream(new FileInputStream(tempFile), resp.getOutputStream(), true);

            } finally {
                resourceStore.deleteTempFile(tempResourceKey);
            }
        } finally {
            securityContext.popUser();
        }
    }
}
