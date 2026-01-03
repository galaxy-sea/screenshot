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

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author ChangJin Wei (魏昌进)
 */
public class ScreenshotConfigurable implements SearchableConfigurable, Configurable.NoScroll {

    SettingsUI settingsUI;

    @Override
    public @NotNull @NonNls String getId() {
        return "screenshot";
    }

    @Override
    public String getDisplayName() {
        return "Screenshot";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsUI = new SettingsUI();
        return settingsUI.getPanel();
    }

    @Override
    public boolean isModified() {
        ScreenshotStateProvider service = ScreenshotStateProvider.getInstance();
        return settingsUI != null && !service.getState().equals(settingsUI.toState());
    }

    @Override
    public void apply() {
        ScreenshotStateProvider service = ScreenshotStateProvider.getInstance();
        service.loadState(settingsUI.toState());
    }

    @Override
    public void reset() {
        ScreenshotStateProvider service = ScreenshotStateProvider.getInstance();
        settingsUI.fromState(service.getState());
    }

    @Override
    public void disposeUIResources() {
        settingsUI = null;
    }
}
