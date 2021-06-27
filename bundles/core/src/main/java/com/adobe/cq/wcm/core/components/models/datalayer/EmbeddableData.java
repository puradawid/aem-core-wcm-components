/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.wcm.core.components.models.datalayer;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Interface defining data for embeddables.
 *
 * @since com.adobe.cq.wcm.core.components.models.datalayer 1.3.0
 */
public interface EmbeddableData extends ComponentData {

    /**
     * Returns the embeddable properties.
     *
     * @return Map of embeddable properties
     *
     * @since com.adobe.cq.wcm.core.components.models.datalayer 1.3.0
     */
    @JsonProperty("embeddableProperties")
    default Map<String, Object> getEmbeddableDetails() {
        return Collections.emptyMap();
    }
}
