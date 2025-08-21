package plus.wcj.jetbrains.plugins.screenshot;

import com.intellij.ide.actions.RevealFileAction;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.ImageUtil;
import org.jetbrains.annotations.NotNull;
import plus.wcj.jetbrains.plugins.screenshot.config.ScreenshotState;
import plus.wcj.jetbrains.plugins.screenshot.config.ScreenshotStateProvider;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author ChangJin Wei (魏昌进)
 */
public class ScreenshotAction extends DumbAwareAction {


    @Override
    public void actionPerformed(AnActionEvent e) {
        boolean left = true;
        ScreenshotState state = ScreenshotStateProvider.getInstance().getState();

        Project project = e.getProject();
        DataContext context = e.getDataContext();
        Editor editor = PlatformDataKeys.EDITOR.getData(context);
        if (editor == null) {
            notifyError(project, "Screenshotting code is only available in an editor");
            return;
        }

        JComponent contentComponent = editor.getContentComponent();
        EditorGutterComponentEx gutterComponent = (EditorGutterComponentEx) editor.getGutter();

        ComponentInfo contentInfo = new ComponentInfo(editor, contentComponent, state);
        ComponentInfo gutterInfo = new ComponentInfo(gutterComponent, contentInfo, state);

        contentInfo.translateXY(contentComponent, contentInfo, gutterInfo, left);
        gutterInfo.translateXY(gutterComponent, contentInfo, gutterInfo, left);

        int imageWidth = gutterInfo.width + contentInfo.width;
        int imageHeight = Math.max(gutterInfo.height, contentInfo.height);

        BufferedImage image = ImageUtil.createImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        try {
            contentInfo.paint(graphics);
            gutterInfo.paint(graphics);

            copyToClipboard(image, state);
            File file = saveImageToDisk(image, state, editor);

            notifyInfo(project, state, file);

        } catch (IOException ex) {
            Messages.showErrorDialog("Failed to capture screenshot: " + ex.getMessage(), "Error");
        } finally {
            if (contentInfo.hasSelection) {
                editor.getSelectionModel().setSelection(contentInfo.selectionStart, contentInfo.selectionEnd);
            }
            graphics.dispose();
        }
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(true);
            return;
        }
        DataContext context = e.getDataContext();
        Editor editor = PlatformDataKeys.EDITOR.getData(context);
        if (editor == null) {
            e.getPresentation().setEnabled(true);
            return;
        }
        SelectionModel selectionModel = editor.getSelectionModel();
        boolean hasSelection = selectionModel.hasSelection();
        if (hasSelection) {
            e.getPresentation().setText("Screenshot Selected Code");
        } else {
            e.getPresentation().setText("Screenshot All Code");
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }


    private File saveImageToDisk(BufferedImage image, ScreenshotState state, Editor editor) throws IOException {
        if (!state.save) {
            return null;
        }
        VirtualFile virtualFile = ((EditorImpl) editor).getVirtualFile();
        String fileName = virtualFile != null ? virtualFile.getName() : "screenshot";

        Path dir = Paths.get(state.outputDir);
        Files.createDirectories(dir);
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        Path out = dir.resolve(String.format("%s_%s.png", fileName, ts));
        File file = out.toFile();
        ImageIO.write(image, "PNG", file);
        return file;
    }

    private void copyToClipboard(BufferedImage image, ScreenshotState state) {
        if (!state.clipboard) {
            return;
        }
        Toolkit.getDefaultToolkit()
               .getSystemClipboard()
               .setContents(new Transferable() {
                   @Override
                   public DataFlavor[] getTransferDataFlavors() {
                       return new DataFlavor[]{DataFlavor.imageFlavor};
                   }

                   @Override
                   public boolean isDataFlavorSupported(DataFlavor flavor) {
                       return flavor == DataFlavor.imageFlavor;
                   }

                   @NotNull
                   @Override
                   public Object getTransferData(DataFlavor flavor) {
                       return image;
                   }
               }, null);
    }


    private void notifyInfo(Project project, ScreenshotState state, File file) {
        NotificationAction saveToFile = null;
        if (state.save) {
            saveToFile = NotificationAction.createSimpleExpiring(file.getName(), () ->
                    RevealFileAction.openFile(file)
            );
        }

        String content = "Please enable Clipboard or set an Output directory in Settings.";
        if (state.clipboard && state.save) {
            content = "Copied to clipboard and saved to:";
        } else if (state.clipboard) {
            content = "Copied to clipboard.";
        } else if (state.save) {
            content = "Saved to:";
        } else {
            saveToFile = NotificationAction.createSimpleExpiring("Open Screenshot Pro settings", () ->
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Screenshot Pro"));
        }

        Notification notification = NotificationGroupManager.getInstance()
                                                            .getNotificationGroup("Screenshot Pro")
                                                            .createNotification("Screenshot Pro", content, NotificationType.INFORMATION);
        if (saveToFile != null) {
            notification.addAction(saveToFile);
        }

        notification.notify(project);
    }

    private static void notifyError(Project project, String content) {
        NotificationGroupManager.getInstance()
                                .getNotificationGroup("Screenshot Pro")
                                .createNotification("Screenshot Pro", content, NotificationType.ERROR)
                                .notify(project);
    }


}
