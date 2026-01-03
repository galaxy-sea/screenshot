/*
 * Copyright (C) 2025-present The original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package plus.wcj.jetbrains.plugins.screenshot.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.SettingsCategory;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author ChangJin Wei (魏昌进)
 */
@State(name = "ScreenshotPro", storages = {@Storage(value = "screenshotPro.xml")}, category = SettingsCategory.PLUGINS)
public class ScreenshotStateProvider implements PersistentStateComponent<ScreenshotState> {

    ScreenshotState config = new ScreenshotState();

    public static ScreenshotStateProvider getInstance() {
        return ApplicationManager.getApplication().getService(ScreenshotStateProvider.class);
    }

    @Override
    @NotNull
    public ScreenshotState getState() {
        this.config.loadStateInit();
        return this.config;
    }

    @Override
    public void loadState(@NotNull ScreenshotState state) {
        XmlSerializerUtil.copyBean(state, this.config);
        this.config.loadStateInit();
    }


}
