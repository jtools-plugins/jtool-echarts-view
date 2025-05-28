package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.json.json5.Json5FileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.lhstack.tools.plugins.IPlugin;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author reisen7
 * @date 2025/5/28 9:00
 * @description
 */

public class PluginImpl implements IPlugin {
    private static final Map<String, JComponent> cache = new HashMap<>();

    private static final Map<String, Disposable> disposable = new HashMap<>();

    private static final String ACTION_PLACE = "JTools-Format";
    private static final Logger log = LoggerFactory.getLogger(PluginImpl.class);

    public String getHtmlDataUri(String resourceName) throws IOException {
        InputStream in = getClass().getResourceAsStream("/" + resourceName);
        if (in == null)
            throw new FileNotFoundException(resourceName + " not found in jar!");
        byte[] bytes = in.readAllBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:text/html;base64," + base64;
    }

    @Override
    public JComponent createPanel(Project project) {
        return cache.computeIfAbsent(project.getLocationHash(), key -> {
            SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true);

            // 创建主面板，使用 BorderLayout
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setPreferredSize(new Dimension(300, 600));
            // 顶部操作面板
            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            JButton visualizeButton = new JButton("可视化");
            topPanel.add(visualizeButton);

            // JCEF 浏览器，data url 加载本地 HTML 和 JS
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
                JOptionPane.showMessageDialog(panel, "加载 HTML/JS 资源失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                throw new RuntimeException(ex);
            }

            // 可视化按钮事件，自动读取编辑器选中内容
            JBCefBrowser finalBrowser = browser;
            visualizeButton.addActionListener(e -> {
                Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                String optionText = null;
                String fileText = null;
                if (editor != null) {
                    optionText = editor.getSelectionModel().getSelectedText();
                    fileText = editor.getDocument().getText();
                }
                if (optionText == null || optionText.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(panel, "请先在代码区选中 ECharts option（JSON）！", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 智能 mock data 字段
                optionText = OptionMocker.mockOption(optionText, fileText);

                finalBrowser.getCefBrowser().executeJavaScript(
                        "window.renderECharts(" + escapeForJS(optionText) + ");",
                        finalBrowser.getCefBrowser().getURL(), 0);
            });

            // 主面板布局
            mainPanel.add(topPanel, BorderLayout.NORTH);
            mainPanel.add(browser.getComponent(), BorderLayout.CENTER);

            panel.setContent(mainPanel);
            return panel;
        });
    }

    // 生成 JS 的 data url
    private String getJsDataUri(String resourceName) throws Exception {
        InputStream in = getClass().getResourceAsStream("/" + resourceName);
        if (in == null)
            throw new FileNotFoundException(resourceName + " not found in jar!");
        byte[] bytes = in.readAllBytes();
        return "data:application/javascript;base64," + java.util.Base64.getEncoder().encodeToString(bytes);
    }

    // 补全 series 的 mock 数据
    private void completeSeries(JsonNode series) {
        if (!series.has("type")) return;
        String type = series.get("type").asText();
        com.fasterxml.jackson.databind.node.ObjectNode s = (com.fasterxml.jackson.databind.node.ObjectNode) series;
        if (!s.has("data")) {
            switch (type) {
                case "line":
                case "bar":
                    s.putArray("data").add(12).add(23).add(34).add(45).add(56);
                    break;
                case "pie":
                    s.putArray("data")
                        .add(mapOf(mapOfEntry("name", "A"), mapOfEntry("value", 10)))
                        .add(mapOf(mapOfEntry("name", "B"), mapOfEntry("value", 20)))
                        .add(mapOf(mapOfEntry("name", "C"), mapOfEntry("value", 30)));
                    break;
                case "scatter":
                    s.putArray("data")
                        .add(arrayOf(10, 20))
                        .add(arrayOf(20, 30))
                        .add(arrayOf(30, 40));
                    break;
            }
        }
    }

    // 工具方法：生成 ObjectNode
    private com.fasterxml.jackson.databind.node.ObjectNode mapOf(Map<String, Object>... entries) {
        com.fasterxml.jackson.databind.node.ObjectNode node = new ObjectMapper().createObjectNode();
        for (Map<String, Object> entry : entries) {
            for (Map.Entry<String, Object> e : entry.entrySet()) {
                if (e.getValue() instanceof Integer) {
                    node.put(e.getKey(), (Integer) e.getValue());
                } else if (e.getValue() instanceof String) {
                    node.put(e.getKey(), (String) e.getValue());
                }
            }
        }
        return node;
    }
    private Map<String, Object> mapOfEntry(String k, Object v) {
        Map<String, Object> m = new HashMap<>();
        m.put(k, v);
        return m;
    }
    private com.fasterxml.jackson.databind.node.ArrayNode arrayOf(int... vals) {
        com.fasterxml.jackson.databind.node.ArrayNode arr = new ObjectMapper().createArrayNode();
        for (int v : vals) arr.add(v);
        return arr;
    }
    // JS 字符串转义，确保 JSON 能安全传递到 JS
    private String escapeForJS(String json) {
        try {
            return new ObjectMapper().writeValueAsString(json);
        } catch (Exception e) {
            return "''";
        }
    }

    @Override
    public void closeProject(String locationHash) {
        cache.remove(locationHash);
        Optional.ofNullable(disposable.remove(locationHash)).ifPresent(Disposer::dispose);
    }

    @Override
    public Icon pluginIcon() {
        return IconLoader.findIcon("format.svg", PluginImpl.class);
    }

    @Override
    public Icon pluginTabIcon() {
        return IconLoader.findIcon("format_tab.svg", PluginImpl.class);
    }

    @Override
    public String pluginName() {
        return "echarts-view";
    }

    @Override
    public String pluginDesc() {
        return "这是一个echarts可视化的插件";
    }

    @Override
    public String pluginVersion() {
        return "0.0.1";
    }
}


