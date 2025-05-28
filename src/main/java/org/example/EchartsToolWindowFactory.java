package org.example;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public class EchartsToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建主面板，使用 BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(300, 600));
        // 顶部操作面板
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton visualizeButton = new JButton("可视化");
        topPanel.add(visualizeButton);

        // JCEF 浏览器
        JBCefBrowser browser = null;
        try {
            browser = new JBCefBrowser(getHtmlDataUri("echarts_view.html"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JScrollPane browserPane = new JScrollPane(browser.getComponent());
        browserPane.setPreferredSize(new Dimension(350, 400));

        // 可视化按钮事件，自动读取编辑器选中内容
        JBCefBrowser finalBrowser = browser;
        visualizeButton.addActionListener(e -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            String optionText = null;
            if (editor != null) {
                optionText = editor.getSelectionModel().getSelectedText();
            }
            if (optionText == null || optionText.trim().isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel, "请先在代码区选中 ECharts option（JSON）！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            finalBrowser.getCefBrowser().executeJavaScript(
                    "window.renderECharts(" + escapeForJS(optionText) + ");",
                    finalBrowser.getCefBrowser().getURL(), 0);
        });

        // 主面板布局
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(browser.getComponent(), BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    // 直接搬运 PluginImpl 的 getHtmlDataUri 和 escapeForJS
    private String getHtmlDataUri(String resourceName) throws IOException {
        InputStream in = getClass().getResourceAsStream("/" + resourceName);
        if (in == null)
            throw new FileNotFoundException(resourceName + " not found in jar!");
        byte[] bytes = in.readAllBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:text/html;base64," + base64;
    }

    private String escapeForJS(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(json);
        } catch (Exception e) {
            return "''";
        }
    }
} 