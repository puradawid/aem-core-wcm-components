/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2017 Adobe
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
package com.adobe.cq.wcm.core.components.internal.servlets;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.factory.ModelFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.adobe.cq.wcm.core.components.internal.LocalizationUtils;
import com.adobe.cq.wcm.core.components.internal.models.v1.SearchImpl;
import com.adobe.cq.wcm.core.components.models.Search;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

import static com.adobe.cq.wcm.core.components.models.ExperienceFragment.PN_FRAGMENT_VARIATION_PATH;

class SearchComponentProvider {

    /**
     * Name of the template structure node.
     */
    private static final String NN_STRUCTURE = "structure";
    
    private final ModelFactory modelFactory;
    private final LanguageManager languageManager;
    private final LiveRelationshipManager relationshipManager;
    
    SearchComponentProvider(ModelFactory modelFactory, LanguageManager languageManager, LiveRelationshipManager liveRelationshipManager) {
        this.modelFactory = modelFactory;
        this.languageManager = languageManager;
        this.relationshipManager = liveRelationshipManager;
    }

    /**
     * Gets the search component for the given request.
     *
     * @param request The search request.
     * @param currentPage The current page.
     * @return The search component.
     */
    @NotNull
    Search getSearchComponent(@NotNull final SlingHttpServletRequest request, @NotNull final Page currentPage) {
        String suffix = request.getRequestPathInfo().getSuffix();
        String relativeContentResourcePath = Optional.ofNullable(suffix)
            .filter(path -> StringUtils.startsWith(path, "/"))
            .map(path -> StringUtils.substring(path, 1))
            .orElse(suffix);

        return Optional.ofNullable(relativeContentResourcePath)
            .filter(StringUtils::isNotEmpty)
            .map(rcrp -> getSearchComponentResourceFromPage(request.getResource(), rcrp)
                .orElse(getSearchComponentResourceFromTemplate(currentPage, rcrp)
                    .orElse(null)))
            .map(resource -> modelFactory.getModelFromWrappedRequest(request, resource, Search.class))
            .orElseGet(() -> new DefaultSearch(currentPage, request.getResourceResolver()));
    }

    /**
     * Gets the search component resource from the page. Looks inside experience fragments in the page too.
     *
     * @param pageResource The page resource.
     * @param relativeContentResourcePath The relative path of the search component resource.
     * @return The search component resource.
     */
    private Optional<Resource> getSearchComponentResourceFromPage(@NotNull final Resource pageResource, final String relativeContentResourcePath) {
        return Optional.ofNullable(Optional.ofNullable(pageResource.getChild(relativeContentResourcePath))
            .orElse(getSearchComponentResourceFromFragments(pageResource.getChild(NameConstants.NN_CONTENT), relativeContentResourcePath)
                .orElse(null)));
    }

    /**
     * Gets the search component resource from the page's template. Looks inside experience fragments in the template too.
     *
     * @param currentPage The current page, whose template will be used.
     * @param relativeContentResourcePath The relative path of the search component resource.
     * @return The search component resource.
     */
    private Optional<Resource> getSearchComponentResourceFromTemplate(@NotNull final Page currentPage, final String relativeContentResourcePath) {
        return Optional.ofNullable(currentPage.getTemplate())
            .map(Template::getPath)
            .map(currentPage.getContentResource().getResourceResolver()::getResource)
            .map(templateResource -> Optional.ofNullable(templateResource.getChild(NN_STRUCTURE + "/" + relativeContentResourcePath))
                .orElse(getSearchComponentResourceFromFragments(templateResource, relativeContentResourcePath)
                    .orElse(null)));
    }

    /**
     * Gets the search component resource from experience fragments under the resource. Walks down the descendants tree.
     *
     * @param resource The resource where experience fragments with search component would be looked up.
     * @param relativeContentResourcePath The relative path of the search component resource.
     * @return The search component resource.
     */
    private Optional<Resource> getSearchComponentResourceFromFragments(Resource resource, String relativeContentResourcePath) {
        return Optional.ofNullable(resource)
            .map(res -> getSearchComponentResourceFromFragment(res, relativeContentResourcePath)
                .orElse(StreamSupport.stream(res.getChildren().spliterator(), false)
                    .map(child -> getSearchComponentResourceFromFragments(child, relativeContentResourcePath).orElse(null))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null)));
    }

    /**
     * Gets the search component resource from a candidate experience fragment component resource.
     *
     * @param candidate The candidate experience fragment component resource.
     * @param relativeContentResourcePath The relative path of the search component resource.
     * @return The search component resource.
     */
    private Optional<Resource> getSearchComponentResourceFromFragment(Resource candidate, String relativeContentResourcePath) {
        return Optional.ofNullable(candidate)
            .map(Resource::getValueMap)
            .map(properties -> properties.get(PN_FRAGMENT_VARIATION_PATH, String.class))
            .map(path -> candidate.getResourceResolver().getResource(path + "/" + relativeContentResourcePath));
    }

    /**
     * A fall-back implementation of the Search model.
     */
    private final class DefaultSearch implements Search {

        @NotNull
        private final String searchRootPagePath;

        /**
         * Construct the default search.
         *
         * @param currentPage The current page.
         * @param resourceResolver The resource resolver.
         */
        public DefaultSearch(@NotNull final Page currentPage, @NotNull final ResourceResolver resourceResolver) {
            this.searchRootPagePath = Optional.ofNullable(currentPage.getContentResource())
                .map(languageManager::getLanguageRoot)
                .map(Page::getPath)
                .map(languageRoot -> LocalizationUtils.getLocalPage(languageRoot, currentPage, resourceResolver, languageManager, relationshipManager)
                    .map(Page::getPath)
                    .orElse(languageRoot)
                )
                .orElseGet(currentPage::getPath);
        }

        @Override
        @Nullable
        public String getId() {
            return null;
        }

        @Override
        public int getResultsSize() {
            return SearchImpl.PROP_RESULTS_SIZE_DEFAULT;
        }

        @Override
        public int getSearchTermMinimumLength() {
            return SearchImpl.PROP_SEARCH_TERM_MINIMUM_LENGTH_DEFAULT;
        }

        @NotNull
        @Override
        public String getSearchRootPagePath() {
            return this.searchRootPagePath;
        }

    }
}
