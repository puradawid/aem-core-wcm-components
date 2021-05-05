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

package com.adobe.cq.wcm.core.components.it.seljup.components.contentfragment;

import com.adobe.cq.testing.selenium.pagewidgets.Helpers;
import com.adobe.cq.testing.selenium.pagewidgets.coral.CoralPopOver;
import com.adobe.cq.testing.selenium.pagewidgets.coral.CoralSelectList;
import com.adobe.cq.testing.selenium.pagewidgets.coral.Dialog;
import com.adobe.cq.testing.selenium.pagewidgets.cq.AutoCompleteField;
import com.adobe.cq.testing.selenium.utils.KeyboardShortCuts;
import com.adobe.cq.wcm.core.components.it.seljup.constant.CoreComponentConstants;
import com.adobe.cq.wcm.core.components.it.seljup.util.Commons;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class ContentFragmentEditDialog extends Dialog {

    private static String content = "cq-dialog-content";
    private static String fragmentPath = "[name='./fragmentPath']";
    private static SelenideElement properties = $("coral-tab[data-foundation-tracking-event*='properties']");
    private static String tagNames = "[name='./tagNames']";
    private static String elementNameSelectButton = "coral-select[name='./elementNames']  > button";
    private static String elementNames = "./elementNames";

    /**
     * Opens the properties tab in editor dialog
     */
    public void openProperties() {
        properties.click();
    }

    public void addElement(String value) {
        $("[coral-multifield-add]").click();
        $$("coral-multifield-item").last().$(elementNameSelectButton).click();
        CoralPopOver popOver =  CoralPopOver.firstOpened();
        popOver.waitVisible();
        Helpers.waitForElementAnimationFinished(popOver.getCssSelector());
        CoralSelectList selectList = new CoralSelectList(popOver.element());
        selectList.selectByValue(value);
    }

    public String getFragmentPath() {
        return fragmentPath;
    }


    public void setFragmentPath(String value) throws InterruptedException {
        AutoCompleteField autoCompleteField = new AutoCompleteField("css:" + fragmentPath);
        autoCompleteField.sendKeys(value);
        KeyboardShortCuts.keySpace();
        Commons.webDriverWait(CoreComponentConstants.WEBDRIVER_WAIT_TIME_MS);
    }

}
