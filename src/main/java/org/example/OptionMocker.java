package org.example;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author reisen7
 * @date 2025/5/29 1:42
 * @description 
 */

public class OptionMocker {
    /**
     * 智能 mock option 里的 data 字段（变量、表达式等），返回 mock 后的 option 字符串
     * @param optionText 选中的 option 字符串
     * @param fileText 整个文件内容（可为 null）
     * @return mock 后的 option 字符串
     */
    public static String mockOption(String optionText, String fileText) {
        if (optionText == null || optionText.trim().isEmpty()) return optionText;
        // mock data 字段
        Pattern dataPattern = Pattern.compile("data\\s*:\\s*([a-zA-Z0-9_\\.]+)");
        Matcher matcher = dataPattern.matcher(optionText);
        Set<String> dataVars = new HashSet<>();
        while (matcher.find()) {
            String var = matcher.group(1);
            if (!var.startsWith("[") && !var.startsWith("{") && !var.startsWith("\"") && !var.startsWith("'")) {
                dataVars.add(var);
            }
        }
        for (String var : dataVars) {
            String value = null;
            if (fileText != null) {
                Pattern thisPattern = Pattern.compile("this\\." + Pattern.quote(var.replace("this.", "")) + "\\s*=\\s*([\"'\\[].*?)[;,\\n]");
                Matcher thisMatcher = thisPattern.matcher(fileText);
                if (thisMatcher.find()) {
                    value = thisMatcher.group(1);
                }
                if (value == null) {
                    Pattern varPattern = Pattern.compile("(const|let|var)\\s+" + Pattern.quote(var.replace("this.", "")) + "\\s*=\\s*([\"'\\[].*?)[;,\\n]");
                    Matcher varMatcher = varPattern.matcher(fileText);
                    if (varMatcher.find()) {
                        value = varMatcher.group(2);
                    }
                }
            }
            if (value != null) {
                optionText = optionText.replaceAll("data\\s*:\\s*" + Pattern.quote(var), "data: " + value);
            } else {
                optionText = optionText.replaceAll("data\\s*:\\s*" + Pattern.quote(var), "data: [10, 20, 30, 40, 50]");
            }
        }
        return optionText;
    }
} 