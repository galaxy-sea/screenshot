package plus.wcj.jetbrains.plugins.screenshot;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import plus.wcj.jetbrains.plugins.screenshot.config.ScreenshotState;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * @author ChangJin Wei (魏昌进)
 * @since 2025/8/21
 */
class ComponentInfo {

    public int x, y;

    public final int width, height;

    public final JComponent component;

    public final boolean show;

    public int translateX, translateY;

    //
    public boolean hasSelection;

    public int selectionStart, selectionEnd;

    public final boolean diffLeft;

    public ComponentInfo(Editor editor, JComponent contentComponent, ScreenshotState config) {
        Document document = editor.getDocument();
        this.hasSelection = editor.getSelectionModel().hasSelection();
        if (this.hasSelection) {
            SelectionModel selectionModel = editor.getSelectionModel();
            this.selectionStart = selectionModel.getSelectionStart();
            this.selectionEnd = selectionModel.getSelectionEnd();

            Point start = editor.offsetToXY(selectionStart);
            Point end = editor.offsetToXY(selectionEnd);

            this.height = end.y - start.y + editor.getLineHeight();
            this.width = getMaxSelectedLineWidth(editor, selectionStart, selectionEnd) + 24;
            this.translateY = -start.y;
            selectionModel.removeSelection();
        } else {
            int lineCount = document.getLineCount();
            int lastLineOffset = document.getLineEndOffset(lineCount - 1);
            int lineNumber = document.getLineNumber(lastLineOffset);
            int lastLineStartOffset = document.getLineStartOffset(lineNumber);

            Point lastLinePosition = editor.logicalPositionToXY(editor.offsetToLogicalPosition(lastLineStartOffset));
            int lineHeight = editor.getLineHeight();
            this.height = lastLinePosition.y + lineHeight;
            this.width = contentComponent.getPreferredSize().width;
        }
        this.component = contentComponent;
        this.show = true;
        this.diffLeft = false;
    }

    public ComponentInfo(EditorGutterComponentEx gutterComponent, ComponentInfo contentInfo, ScreenshotState config) {
        this.component = gutterComponent;
        if (this.show = config.includeGutter) {
            Dimension preferredSize = gutterComponent.getPreferredSize();
            this.width = preferredSize.width;
            this.height = contentInfo.height;
        } else {
            this.width = 0;
            this.height = 0;
        }
        this.diffLeft = gutterComponent.getLocationOnScreen().getX() > contentInfo.component.getLocationOnScreen().getX();
    }

    public void translateXY(JComponent contentComponent, ComponentInfo contentInfo, ComponentInfo gutterInfo) {
        if (gutterInfo.show) {
            if (gutterInfo.diffLeft) {
                this.x = 0;
                this.translateX = 0;
            } else {
                this.x = gutterInfo.width;
                this.translateX = gutterInfo.width;
            }

        }
    }

    public void translateXY(EditorGutterComponentEx contentComponent, ComponentInfo contentInfo, ComponentInfo gutterInfo) {
        this.translateY = contentInfo.translateY;
        if (gutterInfo.diffLeft) {
            this.x = contentInfo.width;
            this.translateX = contentInfo.width;
        } else {
            this.translateX = 0;
        }
    }


    public void paint(Graphics2D graphics) {
        if (show) {
            graphics.setClip(this.x, this.y, this.width, this.height);
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.translate(this.translateX, this.translateY);
            graphics.setTransform(affineTransform);
            component.paint(graphics);
            graphics.setTransform(new AffineTransform());
        }
    }

    private int getMaxSelectedLineWidth(Editor editor, int selectionStart, int selectionEnd) {
        int maxWidth = 0;

        Document document = editor.getDocument();
        int startLine = document.getLineNumber(selectionStart);
        int endLine = document.getLineNumber(selectionEnd);

        for (int line = startLine; line <= endLine; line++) {
            int lineEnd = document.getLineEndOffset(line);

            int segEnd = Math.min(lineEnd, selectionEnd);
            Point pEnd = editor.visualPositionToXY(editor.offsetToVisualPosition(segEnd));
            maxWidth = Math.max(maxWidth, pEnd.x);
        }
        return maxWidth;
    }
}
