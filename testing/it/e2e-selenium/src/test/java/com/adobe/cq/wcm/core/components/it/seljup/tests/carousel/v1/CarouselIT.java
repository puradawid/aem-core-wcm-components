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

package com.adobe.cq.wcm.core.components.it.seljup.tests.carousel.v1;

import com.adobe.cq.testing.selenium.pageobject.EditorPage;
import com.adobe.cq.testing.selenium.pagewidgets.cq.EditableToolbar;
import com.adobe.cq.wcm.core.components.it.seljup.AuthorBaseUITest;
import com.adobe.cq.wcm.core.components.it.seljup.assertion.EditableToolbarAssertion;
import com.adobe.cq.wcm.core.components.it.seljup.components.carousel.CarouselEditDialog;
import com.adobe.cq.wcm.core.components.it.seljup.components.carousel.v1.Carousel;
import com.adobe.cq.wcm.core.components.it.seljup.components.commons.ChildrenEditor;
import com.adobe.cq.wcm.core.components.it.seljup.components.commons.PanelSelector;
import com.adobe.cq.wcm.core.components.it.seljup.constant.CoreComponentConstants;
import com.adobe.cq.wcm.core.components.it.seljup.util.Commons;
import com.adobe.cq.testing.selenium.pageobject.PageEditorPage;
import com.adobe.cq.testing.selenium.pagewidgets.coral.CoralCheckbox;
import com.adobe.cq.testing.selenium.pagewidgets.cq.InsertComponentDialog;
import com.adobe.cq.testing.selenium.utils.KeyboardShortCuts;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.WebDriverRunner;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.ClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("group2")
public class CarouselIT extends AuthorBaseUITest {


    private String policyPath;
    private String proxyPath;
    private static String componentName = "carousel";
    protected Carousel carousel;
    protected EditorPage editorPage;
    protected String cmpPath;
    protected String testPage;
    private static final String clientlibs = "core.wcm.components.carousel.v1";


    /**
     * Before Test Case
     *
     * 1. create test page
     * 2. create clientlib page policy
     * 3. assign clientlib page policy
     * 4. create the proxy component
     * 5. set cq:isContainer property true
     * 6. add the proxy component to the page
     * 7. open the test page in the editor
     */

    @BeforeEach
    public void setupBeforeEach() throws ClientException {
        // 1.
        testPage = authorClient.createPage("testPage", "Test Page Title", rootPage, defaultPageTemplate).getSlingPath();

        // 2.
        String policySuffix = "/structure/page/new_policy";
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("jcr:title", "New Policy");
        data.put("sling:resourceType", "wcm/core/components/policy/policy");
        data.put("clientlibs", clientlibs);
        String policyPath1 = "/conf/"+ label + "/settings/wcm/policies/core-component/components";
        policyPath = Commons.createPolicy(adminClient, policySuffix, data , policyPath1);

        // 3.
        String policyLocation = "core-component/components";
        String policyAssignmentPath = defaultPageTemplate + "/policies/jcr:content";
        data.clear();
        data.put("cq:policy", policyLocation + policySuffix);
        data.put("sling:resourceType", "wcm/core/components/policies/mappings");
        Commons.assignPolicy(adminClient,"",data, policyAssignmentPath);


        // 4.
        proxyPath = Commons.createProxyComponent(adminClient, Commons.rtCarousel_v1, Commons.proxyPath, null, null);

        // 5.
        data.clear();
        data.put("cq:isContainer","true");
        Commons.editNodeProperties(adminClient, proxyPath, data);

        // 6.
        cmpPath = Commons.addComponent(adminClient, proxyPath,testPage + Commons.relParentCompPath, componentName, null);

        // 7.
        editorPage = new PageEditorPage(testPage);
        editorPage.open();

        //8.
        carousel = new Carousel();

    }

    /**
     * After Test Case
     *
     * 1. delete the test proxy component
     * 2. delete the test page
     * 3. delete the clientlib page policy
     * 4. reassign the default policy
     */

    @AfterEach
    public void cleanupAfterEach() throws ClientException, InterruptedException {
        // 1.
        Commons.deleteProxyComponent(adminClient, proxyPath);

        // 2.
        authorClient.deletePageWithRetry(testPage, true,false, CoreComponentConstants.TIMEOUT_TIME_MS, CoreComponentConstants.RETRY_TIME_INTERVAL,  HttpStatus.SC_OK);

        // 3.
        String policyPath1 = "/conf/"+ label + "/settings/wcm/policies/core-component/components";
        Commons.deletePolicy(adminClient,"/structure/page", policyPath1);

        // 4.
        HashMap<String, String> data = new HashMap<String, String>();
        String policyAssignmentPath = "/conf/core-components/settings/wcm/templates/core-components/policies/jcr:content";
        data.put("cq:policy", "wcm/foundation/components/page/default");
        data.put("sling:resourceType", "wcm/core/components/policies/mappings");
        Commons.assignPolicy(adminClient,"",data, policyAssignmentPath);
    }


    /**
     * Create three items via the children editor
     *
     * 1. open the edit dialog
     * 2. add item via the children editor
     * 3. save the edit dialog
     */

    private ElementsCollection createItems() throws  InterruptedException {
        CarouselEditDialog editDialog = carousel.openEditDialog(cmpPath);
        ChildrenEditor childrenEditor = editDialog.getChildrenEditor();
        childrenEditor.clickAddButton();
        InsertComponentDialog insertComponentDialog = editDialog.getInsertComponentDialog();
        insertComponentDialog.selectComponent("/libs/wcm/foundation/components/responsivegrid");
        childrenEditor.getInputItems().last().sendKeys("item0");
        childrenEditor.clickAddButton();
        insertComponentDialog.selectComponent("/libs/wcm/foundation/components/responsivegrid");
        childrenEditor.getInputItems().last().sendKeys("item1");
        childrenEditor.clickAddButton();
        insertComponentDialog.selectComponent("/libs/wcm/foundation/components/responsivegrid");
        childrenEditor.getInputItems().last().sendKeys("item2");
        Commons.saveConfigureDialog();

        carousel.openEditDialog(cmpPath);
        ElementsCollection items = childrenEditor.getInputItems();
        assertTrue(items.size() == 3, "Number to items added should be 3");
        assertTrue(items.get(0).getValue().equals("item0"), "First input item should be item0");
        assertTrue(items.get(1).getValue().equals("item1"), "Second input item should be item1");
        assertTrue(items.get(2).getValue().equals("item2"), "Third input item should be item2");
        Commons.saveConfigureDialog();

        return items;
    }


    /**
     * Test: Edit Dialog: Add child items
     */
    @Test
    @DisplayName("Test: Edit Dialog: Add child items")
    public void testAddItem() throws InterruptedException {
        createItems();
    }

    /**
     * Test: Edit Dialog : Remove items
     *
     * 1. create new items with titles
     * 2. open the edit dialog
     * 3. remove the first item and save the edit dialog
     * 4. open the edit dialog
     * 5. verify that the first item has been removed
     * 6. verify the expanded items select
     * 7. save the edit dialog
     */

    @Test
    @DisplayName("Test: Edit Dialog : Remove items")
    public void testRemoveItem() throws InterruptedException {
        createItems();
        CarouselEditDialog editDialog = carousel.openEditDialog(cmpPath);
        ChildrenEditor childrenEditor = editDialog.getChildrenEditor();
        childrenEditor.removeFirstItem();
        Commons.saveConfigureDialog();

        carousel.openEditDialog(cmpPath);
        ElementsCollection items = childrenEditor.getInputItems();

        assertTrue(items.size() == 2, "Number to items added should be 2");
        assertTrue(items.get(0).getValue().equals("item1"), "First input item should be item1");
        assertTrue(items.get(1).getValue().equals("item2"), "Second input item should be item2");

        Commons.saveConfigureDialog();
    }

    /**
     * Test: Edit Dialog : Reorder items
     *
     * 1. create new items with titles
     * 2. open the edit dialog
     * 3. move the last item before the first one
     * 4. save the edit dialog
     * 5. open the edit dialog
     * 6. verify the new order
     * 7. verify the expanded items select
     * 8. save the edit dialog
     */

    @Test
    @DisplayName("Test: Edit Dialog : Reorder items")
    public void testReorderItem() throws InterruptedException {
        createItems();
        CarouselEditDialog editDialog = carousel.openEditDialog(cmpPath);
        ChildrenEditor childrenEditor = editDialog.getChildrenEditor();
        childrenEditor.moveItems(2,0);
        Commons.saveConfigureDialog();

        carousel.openEditDialog(cmpPath);
        ElementsCollection items = childrenEditor.getInputItems();

        assertTrue(items.size() == 3, "Number to items added should be 3");
        assertTrue(items.get(0).getValue().equals("item2") || items.get(0).getValue().equals("item0"), "First input item should be item2 or item0");
        assertTrue(items.get(1).getValue().equals("item0") || items.get(1).getValue().equals("item2"), "Second input item should be item0 or item2  ");
        assertTrue(items.get(2).getValue().equals("item1"), "Second input item should be item1");

        Commons.saveConfigureDialog();
    }

    @Test
    @DisplayName("Test: Autoplay group toggle")
    public void testAutoplayGroup() throws InterruptedException {
        createItems();
        CarouselEditDialog editDialog = carousel.openEditDialog(cmpPath);
        editDialog.openEditDialogProperties();

        CoralCheckbox autoplay = editDialog.getAutoplay();
        assertTrue(autoplay.isChecked() == false, "Autoplay should be unchecked");
        assertTrue(editDialog.getAutoplayGroup().isDisplayed() == false, "Autoplay Group should not be visible");

        autoplay.setSelected(true);

        assertTrue(editDialog.getAutoplayGroup().isDisplayed() == true, "Autoplay Group should be visible");

        autoplay.setSelected(false);

        assertTrue(editDialog.getAutoplayGroup().isDisplayed() == false, "Autoplay Group should not be visible");

    }


    /**
     * Test: Panel Select
     */
    @Test
    @DisplayName("Test: Panel Select")
    public void testPanelSelect() throws InterruptedException {
        String component = "[data-type='Editable'][data-path='" + cmpPath +"']";
        WebDriver webDriver = WebDriverRunner.getWebDriver();
        new WebDriverWait(webDriver, CoreComponentConstants.TIMEOUT_TIME_SEC).until(ExpectedConditions.elementToBeClickable(By.cssSelector(component)));
        Commons.openEditableToolbar(cmpPath);
        assertTrue(!Commons.isPanelSelectPresent(), "Panel Select should not be present");
        createItems();
        Commons.openEditableToolbar(cmpPath);
        Commons.webDriverWait(CoreComponentConstants.WEBDRIVER_WAIT_TIME_MS);
        assertTrue(Commons.isPanelSelectPresent(), "Panel Select should be present");
        Commons.openPanelSelect();
        Commons.webDriverWait(CoreComponentConstants.WEBDRIVER_WAIT_TIME_MS);
        PanelSelector panelSelector = new PanelSelector();
        assertTrue(panelSelector.isVisible(), "Panel selector should be visible");

        Commons.webDriverWait(CoreComponentConstants.WEBDRIVER_WAIT_TIME_MS);
        ElementsCollection items = panelSelector.getItems();

        assertTrue(items.size() == 3, "Number to items added should be 3");
        assertTrue(items.get(0).getText().contains("item0"), "First panel select items should be item0");
        assertTrue(items.get(1).getText().contains("item1"), "Second panel select item should be item1");
        assertTrue(items.get(2).getText().contains("item2"), "Second panel select item should be item2");

        Commons.switchContext("ContentFrame");
        assertTrue(carousel.getIndicators().get(0).getText().contains("item0"),"First indicator item should be item0");
        Commons.switchToDefaultContext();

        webDriver = WebDriverRunner.getWebDriver();
        new WebDriverWait(webDriver, CoreComponentConstants.TIMEOUT_TIME_SEC).until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(panelSelector.getCssSelector())));

        //4.
        panelSelector.reorderItems(0, 2);

        Commons.switchContext("ContentFrame");
        //wait for the reordering to reflect
        Commons.webDriverWait(CoreComponentConstants.WEBDRIVER_WAIT_TIME_MS);
        assertTrue(carousel.getIndicators().get(2).getText().contains("item0"),"Third indicator item should be item0 after re-order");
        Commons.switchToDefaultContext();

        carousel.getCQOverlay().openPlaceholder(testPage);
        panelSelector = new PanelSelector();
        assertTrue(panelSelector.isVisible() == false, "Panel selector should not be visible");
    }


    /**
     * Test: Accessibility : Navigate Right
     */
    @Test
    @DisplayName("Test: Accessibility : Navigate Right")
    public void testAccessibilityNavigateRight() throws InterruptedException {
        createItems();
        editorPage.enterPreviewMode();
        Commons.switchContext("ContentFrame");
        carousel.getIndicators().get(0).click();
        assertTrue(carousel.isIndicatorActive(0),"first element should be active");
        KeyboardShortCuts.keyRight();
        assertTrue(carousel.isIndicatorActive(1),"Second element should be active");
        KeyboardShortCuts.keyRight();
        assertTrue(carousel.isIndicatorActive(2),"Third element should be active");
    }

    /**
     * Test: Accessibility : Navigate Left
     */
    @Test
    @DisplayName("Test: Accessibility : Navigate Left")
    public void testAccessibilityNavigateLeft() throws InterruptedException {
        createItems();
        editorPage.enterPreviewMode();
        Commons.switchContext("ContentFrame");
        carousel.getIndicators().get(2).click();
        assertTrue(carousel.isIndicatorActive(2),"Third element should be active");
        KeyboardShortCuts.keyLeft();
        assertTrue(carousel.isIndicatorActive(1),"Second element should be active");
        KeyboardShortCuts.keyLeft();
        assertTrue(carousel.isIndicatorActive(0),"First element should be active");
    }

    /**
     * Test: Keys : Navigate end / start
     */
    @Test
    @DisplayName("Test: Accessibility : Navigate Left")
    public void testAccessibilityNavigateEndStart() throws InterruptedException {
        createItems();
        editorPage.enterPreviewMode();
        Commons.switchContext("ContentFrame");
        carousel.getIndicators().get(0).click();
        assertTrue(carousel.isIndicatorActive(0),"First element should be active");
        KeyboardShortCuts.keyEnd();
        assertTrue(carousel.isIndicatorActive(2),"Third element should be active");
        KeyboardShortCuts.keyStart();
        assertTrue(carousel.isIndicatorActive(0),"First element should be active");
    }

    /**
     * Test: Allowed components
     */
    @Test
    @DisplayName("Test: Allowed components")
    public void testAllowedComponents() throws ClientException, InterruptedException, TimeoutException {
        String teaserProxyPath = Commons.createProxyComponent(adminClient, Commons.rtTeaser_v1, Commons.proxyPath, null, null);
        String policySuffix = "/carousel/new_policy";
        HashMap<String, String> data = new HashMap<String, String>();
        data.clear();
        data.put("jcr:title", "New Policy");
        data.put("sling:resourceType", "wcm/core/components/policy/policy");
        data.put("components",teaserProxyPath);
        String policyPath1 = "/conf/"+ label + "/settings/wcm/policies/core-component/components";
        policyPath = Commons.createPolicy(adminClient, policySuffix, data , policyPath1);

        // add a policy for carousel component
        String policyLocation = "core-component/components";
        String policyAssignmentPath = defaultPageTemplate + "/policies/jcr:content/root/responsivegrid/core-component/components";
        data.clear();
        data.put("cq:policy", policyLocation + policySuffix);
        data.put("sling:resourceType", "wcm/core/components/policies/mappings");
        Commons.assignPolicy(adminClient,"/carousel",data, policyAssignmentPath, 200, 201);


        String testPage = authorClient.createPage("testPage", "Test Page Title", rootPage, defaultPageTemplate).getSlingPath();

        String compPath = Commons.addComponent(adminClient, proxyPath, testPage + Commons.relParentCompPath, "carousel", null);

        // open test page in page editor
        editorPage = new PageEditorPage(testPage);
        editorPage.open();

        String component = "[data-type='Editable'][data-path='" + compPath +"']";
        final WebDriver webDriver = WebDriverRunner.getWebDriver();
        new WebDriverWait(webDriver, CoreComponentConstants.TIMEOUT_TIME_SEC).until(ExpectedConditions.elementToBeClickable(By.cssSelector(component)));
        EditableToolbar editableToolbar = editorPage.openEditableToolbar(compPath);

        //2.
        EditableToolbarAssertion editableToolbarAssertion = new EditableToolbarAssertion(editableToolbar,
            "editable toolbar of none style selector enabled component - %s button is displayed while it should not");

        editableToolbarAssertion.assertInsertButton(true);

        editableToolbar.getInsertButton().click();
        Commons.webDriverWait(CoreComponentConstants.WEBDRIVER_WAIT_TIME_MS);
        assertTrue(Commons.isComponentPresentInInsertDialog(teaserProxyPath), "teaser component should be present in insert dialog");
        Commons.deleteProxyComponent(adminClient, teaserProxyPath);
    }


}
