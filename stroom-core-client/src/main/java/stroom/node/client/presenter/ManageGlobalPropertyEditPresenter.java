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

package stroom.node.client.presenter;

import stroom.dispatch.client.AsyncCallbackAdaptor;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.entity.client.presenter.ManageEntityEditPresenter;
import stroom.entity.shared.EntityServiceSaveAction;
import stroom.node.shared.GlobalProperty;
import stroom.security.client.ClientSecurityContext;
import stroom.widget.popup.client.presenter.PopupSize;
import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;

public final class ManageGlobalPropertyEditPresenter
        extends ManageEntityEditPresenter<ManageGlobalPropertyEditPresenter.GlobalPropertyEditView, GlobalProperty> {
    final ClientDispatchAsync dispatcher;

    @Inject
    public ManageGlobalPropertyEditPresenter(final EventBus eventBus, final GlobalPropertyEditView view,
                                             final ClientDispatchAsync dispatcher, final ClientSecurityContext securityContext) {
        super(eventBus, dispatcher, view, securityContext);
        this.dispatcher = dispatcher;
    }

    @Override
    protected String getEntityType() {
        return GlobalProperty.ENTITY_TYPE;
    }

    @Override
    protected String getEntityDisplayType() {
        return "Application Property";
    }

    @Override
    protected void read() {
        getView().setEditable(getEntity().isEditable());// && isCurrentUserUpdate());
        getView().setPasswordStyle(getEntity().isPassword());
        getView().setRequireRestart(getEntity().isRequireRestart());
        getView().setRequireUiRestart(getEntity().isRequireUiRestart());
        getView().getName().setText(getEntity().getName());
        getView().getValue().setText(getEntity().getValue());
        getView().getDescription().setText(getEntity().getDescription());
        getView().getDefaultValue().setText(getEntity().getDefaultValue());
        getView().getSource().setText(getEntity().getSource());
    }

    @Override
    protected void write(final boolean hideOnSave) {
        getEntity().setValue(getView().getValue().getText());

        // Save the device.
        dispatcher.execute(new EntityServiceSaveAction<GlobalProperty>(getEntity()),
                new AsyncCallbackAdaptor<GlobalProperty>() {
                    @Override
                    public void onSuccess(final GlobalProperty result) {
                        setEntity(result);
                        if (hideOnSave) {
                            hide();
                        }
                    }
                });

    }

    @Override
    protected PopupSize getPopupSize() {
        return new PopupSize(550, 340, 550, 340, 1024, 340, true);
    }

    public interface GlobalPropertyEditView extends View {
        HasText getName();

        HasText getValue();

        HasText getDefaultValue();

        HasText getDescription();

        HasText getSource();

        void setEditable(boolean edit);

        void setPasswordStyle(boolean password);

        void setRequireRestart(boolean requiresRestart);

        void setRequireUiRestart(boolean requiresRestart);
    }
}
