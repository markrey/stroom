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

package stroom.entity.server;

import stroom.entity.shared.BaseEntity;
import stroom.entity.shared.EntityService;
import stroom.entity.shared.EntityServiceException;
import stroom.entity.shared.HasName;
import stroom.entity.shared.ProvidesNamePattern;
import stroom.util.shared.EqualsUtil;

public class NameValidationUtil {
    public static void validate(final EntityService<?> entityService, final BaseEntity entity) {
        validate(entityService, null, entity);
    }

    public static void validate(final EntityService<?> entityService, final BaseEntity before, final BaseEntity after) {
        if (after != null && after instanceof HasName) {
            // Validate the entity name if it has been changed.
            if (before != null && before instanceof HasName) {
                if (!EqualsUtil.isEquals(((HasName) before).getName(), ((HasName) after).getName())) {
                    validate(entityService, ((HasName) after).getName());
                }
            } else {
                validate(entityService, ((HasName) after).getName());
            }
        }
    }

    public static void validate(final EntityService<?> entityService, final String name) {
        if (entityService != null && entityService instanceof ProvidesNamePattern) {
            final ProvidesNamePattern providesNamePattern = (ProvidesNamePattern) entityService;
            final String pattern = providesNamePattern.getNamePattern();
            if (pattern != null && pattern.length() > 0) {
                if (name == null || !name.matches(pattern)) {
                    throw new EntityServiceException("Invalid name \"" + name + "\" ("
                            + pattern + ")");
                }
            }
        }
    }
}
