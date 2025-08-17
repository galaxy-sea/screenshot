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
