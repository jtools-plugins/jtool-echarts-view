package org.example;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.LanguageTextField;
import org.jetbrains.annotations.NotNull;

/**
 * @author reisen7
 * @date 2025/5/28 9:00
 * @description
 */

public class DynamicLanguageTextField extends LanguageTextField implements Disposable {
    private final SimpleDocumentCreator documentCreator;

    private LanguageFileType languageFileType;

    public DynamicLanguageTextField(LanguageFileType languageFileType, Project project, String title) {
        this(languageFileType, project, new SimpleDocumentCreator() {
            @Override
            public void customizePsiFile(PsiFile file) {
                file.setName(title);
            }
        });
    }

    public DynamicLanguageTextField(LanguageFileType languageFileType, Project project) {
        this(languageFileType, project, new SimpleDocumentCreator());
    }

    public DynamicLanguageTextField(LanguageFileType languageFileType, Project project, SimpleDocumentCreator documentCreator) {
        super(languageFileType.getLanguage(), project, "", documentCreator, false);
        this.documentCreator = documentCreator;
        this.languageFileType = languageFileType;
    }


    public void changeLanguageFileType(LanguageFileType languageFileType) {
        if (this.languageFileType != languageFileType) {
            this.setNewDocumentAndFileType(languageFileType, this.documentCreator.createDocument(this.getDocument().getText(), languageFileType.getLanguage(), this.getProject()));
            this.languageFileType = languageFileType;
            Editor editor = this.getEditor();
            if (editor instanceof EditorEx) {
                EditorEx editorEx = (EditorEx) editor;
                editorEx.setHighlighter(HighlighterFactory.createHighlighter(this.getProject(), this.languageFileType));
            }
        }
    }

    public LanguageFileType getLanguageFileType() {
        return languageFileType;
    }

    @Override
    protected @NotNull EditorEx createEditor() {
        EditorEx editor = (EditorEx) EditorFactory.getInstance().createEditor(getDocument(), getProject(), languageFileType, false);
        editor.setHorizontalScrollbarVisible(true);
        editor.setVerticalScrollbarVisible(true);
        editor.setHighlighter(HighlighterFactory.createHighlighter(this.getProject(), this.languageFileType));
        PsiFile psiFile = PsiDocumentManager.getInstance(getProject()).getPsiFile(getDocument());
        if (psiFile != null) {
            DaemonCodeAnalyzer.getInstance(getProject()).setHighlightingEnabled(psiFile, true);
        }
        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setRightMarginShown(true);
        settings.setAutoCodeFoldingEnabled(false);
        settings.setLineMarkerAreaShown(false);
        settings.setAdditionalColumnsCount(1);
        settings.setRightMargin(-1);
        settings.setLineCursorWidth(1);
        settings.setAdditionalLinesCount(0);
        settings.setFoldingOutlineShown(true);
        Disposer.register(getProject(), () -> EditorFactory.getInstance().releaseEditor(editor));
        return editor;
    }

    @Override
    public void dispose() {
        Editor editor = getEditor();
        if (editor != null) {
            EditorFactory.getInstance().releaseEditor(editor);
        }
    }
}
