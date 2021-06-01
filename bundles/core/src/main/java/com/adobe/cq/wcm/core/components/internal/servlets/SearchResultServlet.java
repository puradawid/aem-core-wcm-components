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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.scripting.core.ScriptHelper;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.wcm.core.components.internal.link.LinkHandler;
import com.adobe.cq.wcm.core.components.internal.models.v1.PageListItemImpl;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.models.Search;
import com.day.cq.search.PredicateConverter;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.eval.FulltextPredicateEvaluator;
import com.day.cq.search.eval.PathPredicateEvaluator;
import com.day.cq.search.eval.TypePredicateEvaluator;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Search servlet.
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.selectors=" + SearchResultServlet.DEFAULT_SELECTOR,
        "sling.servlet.resourceTypes=cq/Page",
        "sling.servlet.extensions=json",
        "sling.servlet.methods=GET"
    }
)
public final class SearchResultServlet extends SlingSafeMethodsServlet {

    /**
     * Selector to trigger the search servlet.
     */
    static final String DEFAULT_SELECTOR = "searchresults";

    /**
     * Name of the query parameter containing the user query.
     */
    static final String PARAM_FULLTEXT = "fulltext";

    /**
     * Name of the query parameter indicating the search result offset.
     */
    static final String PARAM_RESULTS_OFFSET = "resultsOffset";

    @Reference
    private transient QueryBuilder queryBuilder;

    @Reference
    private transient LanguageManager languageManager;

    @Reference
    private transient LiveRelationshipManager relationshipManager;

    @Reference
    private transient ModelFactory modelFactory;

    private BundleContext bundleContext;

    @Activate
    protected void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response)
        throws IOException {
        Page currentPage = Optional.ofNullable(request.getResourceResolver().adaptTo(PageManager.class))
            .map(pm -> pm.getContainingPage(request.getResource()))
            .orElse(null);
        if (currentPage != null) {
            SlingBindings bindings = new SlingBindings();
            bindings.setSling(new ScriptHelper(bundleContext, null, request, response));
            request.setAttribute(SlingBindings.class.getName(), bindings);

            Search searchComponent = new SearchComponentProvider(modelFactory, languageManager, relationshipManager)
                .getSearchComponent(request.getRequestPathInfo().getSuffix(), request, currentPage);
            try {
                List<ListItem> results = getResults(request, searchComponent, currentPage.getPageManager());
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                new ObjectMapper().writeValue(response.getWriter(), results);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Gets the search results.
     *
     * @param request The search request.
     * @param searchComponent The search component.
     * @param pageManager A PageManager.
     * @return List of search results.
     */
    @NotNull
    private List<ListItem> getResults(@NotNull final SlingHttpServletRequest request,
                                      @NotNull final Search searchComponent,
                                      @NotNull final PageManager pageManager) {

        List<ListItem> results = new ArrayList<>();
        String fulltext = request.getParameter(PARAM_FULLTEXT);
        if (fulltext == null || fulltext.length() < searchComponent.getSearchTermMinimumLength()) {
            return results;
        }
        long resultsOffset = Optional.ofNullable(request.getParameter(PARAM_RESULTS_OFFSET)).map(Long::parseLong).orElse(0L);
        Map<String, String> predicatesMap = new HashMap<>();
        predicatesMap.put(FulltextPredicateEvaluator.FULLTEXT, fulltext);
        predicatesMap.put(PathPredicateEvaluator.PATH, searchComponent.getSearchRootPagePath());
        predicatesMap.put(TypePredicateEvaluator.TYPE, NameConstants.NT_PAGE);
        PredicateGroup predicates = PredicateConverter.createPredicates(predicatesMap);
        ResourceResolver resourceResolver = request.getResource().getResourceResolver();
        Query query = queryBuilder.createQuery(predicates, resourceResolver.adaptTo(Session.class));
        if (searchComponent.getResultsSize() != 0) {
            query.setHitsPerPage(searchComponent.getResultsSize());
        }
        if (resultsOffset != 0) {
            query.setStart(resultsOffset);
        }
        SearchResult searchResult = query.getResult();

        LinkHandler linkHandler = request.adaptTo(LinkHandler.class);
        // Query builder has a leaking resource resolver, so the following work around is required.
        ResourceResolver leakingResourceResolver = null;
        try {
            Iterator<Resource> resourceIterator = searchResult.getResources();
            while (resourceIterator.hasNext()) {
                Resource resource = resourceIterator.next();

                // Get a reference to QB's leaking resource resolver
                if (leakingResourceResolver == null) {
                    leakingResourceResolver = resource.getResourceResolver();
                }

                Optional.of(resource)
                    .map(res -> resourceResolver.getResource(res.getPath()))
                    .map(pageManager::getContainingPage)
                    .map(page -> new PageListItemImpl(linkHandler, page, searchComponent.getId(),
                        PageListItemImpl.PROP_DISABLE_SHADOWING_DEFAULT, null))
                    .ifPresent(results::add);
            }
        } finally {
            if (leakingResourceResolver != null) {
                leakingResourceResolver.close();
            }
        }
        return results;
    }
}
