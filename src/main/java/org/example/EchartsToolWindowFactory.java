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

/**
 * @author reisen7
 * @date 2025/5/29 1:42
 * @description 
 */

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

        // 只用 data url 加载 HTML
        JBCefBrowser browser;
        try {
            // 读取 HTML 内容
            String html = new String(getClass().getResourceAsStream("/echarts_view.html").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            // 替换静态资源 <script src="static/xxx.js"> 为 data url
            String[] staticFiles = {"echarts.min.js", "index.min.js"};
            for (String file : staticFiles) {
                String tag = "<script src=\"static/" + file + "\"></script>";
                String jsDataUrl = getJsDataUri("static/" + file);
                String inject = "<script src=\"" + jsDataUrl + "\"></script>";
                html = html.replace(tag, inject);
            }
            browser = new JBCefBrowser("data:text/html;base64," + java.util.Base64.getEncoder().encodeToString(html.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainPanel, "加载 HTML/JS 资源失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(ex);
        }
        JScrollPane browserPane = new JScrollPane(browser.getComponent());
        browserPane.setPreferredSize(new Dimension(350, 400));

        // 可视化按钮事件，自动读取编辑器选中内容
        JBCefBrowser finalBrowser = browser;
        visualizeButton.addActionListener(e -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            String optionText = null;
            String fileText = null;
            if (editor != null) {
                optionText = editor.getSelectionModel().getSelectedText();
                try {
                    fileText = editor.getDocument().getText();
                } catch (Exception ex) {
                    fileText = null;
                }
            }
            if (optionText == null || optionText.trim().isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel, "请先在代码区选中 ECharts option（JSON）！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // 智能 mock option
            String mockOption = org.example.OptionMocker.mockOption(optionText, fileText);
            finalBrowser.getCefBrowser().executeJavaScript(
                    "window.renderECharts(" + escapeForJS(mockOption) + ");",
                    finalBrowser.getCefBrowser().getURL(), 0);
        });

        // 主面板布局
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(browser.getComponent(), BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    // 生成 JS 的 data url
    private String getJsDataUri(String resourceName) throws Exception {
        InputStream in = getClass().getResourceAsStream("/" + resourceName);
        if (in == null)
            throw new FileNotFoundException(resourceName + " not found in jar!");
        byte[] bytes = in.readAllBytes();
        return "data:application/javascript;base64," + java.util.Base64.getEncoder().encodeToString(bytes);
    }

    private String escapeForJS(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(json);
        } catch (Exception e) {
            return "''";
        }
    }
}