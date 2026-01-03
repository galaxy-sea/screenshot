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


import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;

import javax.swing.*;
import java.awt.event.ItemEvent;

/**
 * @author ChangJin Wei (魏昌进)
 */
public class SettingsUI {

    public JPanel panel;

    public JBCheckBox includeGutter;

    public JBCheckBox clipboard;

    public JBCheckBox save;

    public TextFieldWithBrowseButton outputDir;


    public JPanel getPanel() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        outputDir.addBrowseFolderListener(
                "Select Screenshot Output Folder",
                "Choose a folder to save code screenshots.",
                null,
                descriptor
        );

        save.addItemListener(itemEvent -> {
            boolean selected = itemEvent.getStateChange() == ItemEvent.SELECTED;
            outputDir.setEnabled(selected);
        });

        return panel;
    }

    public ScreenshotState toState() {
        ScreenshotState configProvider = new ScreenshotState();
        configProvider.includeGutter = this.includeGutter.isSelected();

        configProvider.clipboard = this.clipboard.isSelected();
        configProvider.save = this.save.isSelected();

        configProvider.outputDir = this.outputDir.getText();
        return configProvider;
    }

    public void fromState(ScreenshotState config) {
        this.includeGutter.setSelected(config.includeGutter);
        this.clipboard.setSelected(config.clipboard);
        this.save.setSelected(config.save);
        this.outputDir.setText(config.outputDir);
    }
}
