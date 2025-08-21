package plus.wcj.jetbrains.plugins.screenshot;

import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.UIUtil;
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

    public static final String ID = "Screenshot Pro";

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(true);

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        if (editor.getEditorKind() == EditorKind.DIFF) {
            FrameDiffTool.DiffViewer viewer = e.getData(DiffDataKeys.DIFF_VIEWER);
            if (viewer instanceof TwosideTextDiffViewer) {
                e.getPresentation().setText(editor.getSelectionModel().hasSelection() ? "Screenshot Selected Code" : "Screenshot Diff Code(Left + Right)");
                return;
            }
        }
        e.getPresentation().setText(editor.getSelectionModel().hasSelection() ? "Screenshot Selected Code" : "Screenshot All Code");
    }


    @Override
    public void actionPerformed(AnActionEvent e) {
        ScreenshotState state = ScreenshotStateProvider.getInstance().getState();

        Project project = e.getProject();
        DataContext context = e.getDataContext();
        Editor editor = PlatformDataKeys.EDITOR.getData(context);
        if (editor == null) {
            notifyError(project, "Screenshotting code is only available in an editor");
            return;
        }
        if (editor.getEditorKind() == EditorKind.DIFF && !editor.getSelectionModel().hasSelection()) {
            FrameDiffTool.DiffViewer viewer = e.getData(DiffDataKeys.DIFF_VIEWER);
            if (viewer instanceof TwosideTextDiffViewer twosideTextDiffViewer) {
                screenshot(twosideTextDiffViewer, editor, state, project);
                return;
            }
        }
        screenshot(editor, state, project);
    }

    private void screenshot(TwosideTextDiffViewer twosideTextDiffViewer, Editor editor, ScreenshotState state, Project project) {
        try {
            Color background = editor.getContentComponent().getBackground();

            BufferedImage leftEditor = toBufferedImage(twosideTextDiffViewer.getEditor1(), state, project);
            // BufferedImage splitter = toBufferedImage(twosideTextDiffViewer);
            BufferedImage rightEditor = toBufferedImage(twosideTextDiffViewer.getEditor2(), state, project);
            BufferedImage image = imageMerge(background, leftEditor, rightEditor);

            copyToClipboard(image, state);
            File file = saveImageToDisk(image, state, editor);
            notifyInfo(project, state, file);
        } catch (Exception e) {
            Messages.showErrorDialog("Failed to capture screenshot: " + e.getMessage(), "Error");
        }
    }

    private void screenshot(Editor editor, ScreenshotState state, Project project) {
        try {
            BufferedImage image = toBufferedImage(editor, state, project);
            copyToClipboard(image, state);
            File file = saveImageToDisk(image, state, editor);
            notifyInfo(project, state, file);
        } catch (Exception e) {
            Messages.showErrorDialog("Failed to capture screenshot: " + e.getMessage(), "Error");
        }
    }

    // TODO: ChangJin Wei (魏昌进) 2025/8/21 I don't know how to handle this problem either. 😂
    private BufferedImage toBufferedImage(TwosideTextDiffViewer twosideTextDiffViewer) {
        Splitter splitter = UIUtil.findComponentOfType(twosideTextDiffViewer.getComponent(), Splitter.class);
        JPanel divider = splitter.getDivider();
        BufferedImage image = ImageUtil.createImage(divider.getWidth(), divider.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        divider.paint(graphics);
        return image;
    }

    private BufferedImage toBufferedImage(Editor editor, ScreenshotState state, Project project) {
        JComponent contentComponent = editor.getContentComponent();
        EditorGutterComponentEx gutterComponent = (EditorGutterComponentEx) editor.getGutter();

        ComponentInfo contentInfo = new ComponentInfo(editor, contentComponent, state);
        ComponentInfo gutterInfo = new ComponentInfo(gutterComponent, contentInfo, state);

        contentInfo.translateXY(contentComponent, contentInfo, gutterInfo);
        gutterInfo.translateXY(gutterComponent, contentInfo, gutterInfo);

        int imageWidth = gutterInfo.width + contentInfo.width;
        int imageHeight = Math.max(gutterInfo.height, contentInfo.height);

        BufferedImage image = ImageUtil.createImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            contentInfo.paint(graphics);
            gutterInfo.paint(graphics);
        } finally {
            if (contentInfo.hasSelection) {
                editor.getSelectionModel().setSelection(contentInfo.selectionStart, contentInfo.selectionEnd);
            }
            graphics.dispose();
        }
        return image;
    }

    private BufferedImage imageMerge(Color background, BufferedImage... images) {
        int imageWidth = 0;
        int imageHeight = 0;
        for (BufferedImage image : images) {
            imageWidth += image.getWidth();
            imageHeight = Math.max(imageHeight, image.getHeight());
        }
        BufferedImage imageMerge = ImageUtil.createImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = imageMerge.createGraphics();
        graphics.setColor(background);
        graphics.fillRect(0, 0, imageWidth, imageHeight);

        int x = 0;
        for (BufferedImage image : images) {
            graphics.drawImage(image, x, 0, null); // 顶部对齐
            x += image.getWidth();
        }
        return imageMerge;
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
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, ID));
        }

        Notification notification = NotificationGroupManager.getInstance()
                                                            .getNotificationGroup(ID)
                                                            .createNotification(ID, content, NotificationType.INFORMATION);
        if (saveToFile != null) {
            notification.addAction(saveToFile);
        }

        notification.notify(project);
    }

    private static void notifyError(Project project, String content) {
        NotificationGroupManager.getInstance()
                                .getNotificationGroup(ID)
                                .createNotification(ID, content, NotificationType.ERROR)
                                .notify(project);
    }


}
