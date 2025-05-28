package org.example;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author reisen7
 * @date 2025/5/29 1:57
 * @description 
 */

public class OptionMocker {
    /**
     * 智能 mock option 里的 data 字段（变量、表达式等），只对选区 optionText 做 mock，变量查找用 fileText
     * @param optionText 选中的 option 字符串
     * @param fileText 整个文件内容（可为 null）
     * @return mock 后的 option 字符串
     */
    public static String mockOption(String optionText, String fileText) {
        if (optionText == null || optionText.trim().isEmpty()) return optionText;
        // 1. 在 fileText 里查找所有变量声明（支持多行、数组、对象、字符串、函数体等）
        java.util.Map<String, String> varMap = new java.util.HashMap<>();
        if (fileText != null) {
            java.util.regex.Pattern varPattern = java.util.regex.Pattern.compile("(const|let|var)\\s+([a-zA-Z0-9_]+)\\s*=", java.util.regex.Pattern.MULTILINE);
            java.util.regex.Matcher varMatcher = varPattern.matcher(fileText);
            while (varMatcher.find()) {
                String varName = varMatcher.group(2);
                int valueStart = varMatcher.end();
                String value = extractJsValue(fileText, valueStart);
                if (value != null && !value.isEmpty()) {
                    varMap.put(varName, value.trim());
                }
            }
        }
        // 2. 只对 optionText 里的 data: xxx 做替换（不会影响 optionText 之外内容）
        java.util.regex.Pattern dataPattern = java.util.regex.Pattern.compile("data\\s*:\\s*([a-zA-Z0-9_\\.]+)");
        java.util.regex.Matcher matcher = dataPattern.matcher(optionText);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String var = matcher.group(1);
            String value = varMap.get(var.replace("this.", ""));
            if (value != null) {
                // 只替换变量名，避免把数组/对象/字符串等合法值误替换
                matcher.appendReplacement(sb, "data: " + value);
            } else {
                matcher.appendReplacement(sb, "data: [10, 20, 30, 40, 50]");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 用堆栈法提取 JS 变量声明的完整值（支持数组、对象、字符串、函数体等）
     * @param text 文件内容
     * @param startIdx 变量值起始下标
     * @return 变量值字符串（不含分号）
     */
    private static String extractJsValue(String text, int startIdx) {
        int len = text.length();
        int i = startIdx;
        while (i < len && Character.isWhitespace(text.charAt(i))) i++;
        if (i >= len) return null;
        char first = text.charAt(i);
        java.util.Stack<Character> stack = new java.util.Stack<>();
        boolean inString = false;
        char stringQuote = 0;
        boolean escape = false;
        boolean started = false;
        for (; i < len; i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == stringQuote) {
                    inString = false;
                }
            } else {
                if (c == '"' || c == '\'' || c == '`') {
                    inString = true;
                    stringQuote = c;
                    if (!started) started = true;
                } else if (c == '[') {
                    stack.push(']');
                    if (!started) started = true;
                } else if (c == '{') {
                    stack.push('}');
                    if (!started) started = true;
                } else if (c == '(') {
                    stack.push(')');
                    if (!started) started = true;
                } else if ((c == ']' || c == '}' || c == ')')) {
                    if (!stack.isEmpty() && stack.peek() == c) {
                        stack.pop();
                        if (stack.isEmpty() && started) {
                            i++; // 包含当前闭合符
                            break;
                        }
                    }
                } else if (!started && !Character.isWhitespace(c)) {
                    // 变量值是单个字面量（如数字、true、false、null）
                    started = true;
                } else if (started && stack.isEmpty() && (c == ';' || c == ',' || c == '\n')) {
                    break;
                }
            }
        }
        return text.substring(startIdx, i).trim();
    }
} 