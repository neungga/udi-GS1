package com.jsh.erp.utils;

import com.jsh.erp.exception.BusinessRunTimeException;
import org.apache.commons.lang3.StringUtils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author neungga
 * udi解析（GS1型）
 * 应用标识(AI)     字段      长度	                    意义
 * 01              GTN码	13、14、8和12,一般为14位	GS1规则库中的管理码
 * 10	           生产批号	8~20位	                生产批号
 * 11	           生产日期	6位，YYMMDD	            生产日期
 * 17	           失效日期	6位，YYMMDD	            截止有效期
 * 21	           序列号	0~20位内	                用于追溯个体的唯一标识号
 */
public class UDIReaderUtils {

    private static final String START_PREFIX = "01";
    private static final String MANUFACTURE_PREFIX = "11";
    private static final String EXPIRY_PREFIX = "17";
    private static final String BATCH_PREFIX = "10";
    private static final String SERIAL_PREFIX = "21";
    private static final String EMPTY_VALUE = "";

    public static Map<String, String> parseUdiString(String udiString) {
        if (StringUtils.isBlank(udiString)) {
            throw new BusinessRunTimeException(500,
                    "扫码参数不能为空");
        }

        Map<String, String> result = new HashMap<>(16);
        result.put("gtin", "");
        result.put("manufactureDate", "");
        result.put("expiryDate", "");
        result.put("batch", "");
        result.put("serial", "");
        try {
            if (udiString.startsWith(START_PREFIX)) {
                String DI = udiString.substring(0, 16);
                String gtin = DI.substring(2, 16);
                log("GTIN码: " + gtin);
                result.put("gtin", gtin);

                String PI = udiString.substring(16);
                if (StringUtils.isBlank(PI)) {
                    throw new BusinessRunTimeException(500,
                            "PI不存在");
                }

                String nextPI = parseSection(PI, MANUFACTURE_PREFIX, "生产日期", "manufactureDate", result);

                nextPI = parseSection(nextPI, EXPIRY_PREFIX, "失效日期", "expiryDate", result);

                decomposePI(nextPI, result);
            }
        } catch (Exception e) {
            log("解析错误: " + e.getMessage());
            throw new BusinessRunTimeException(500,
                    "解析错误: " + e.getMessage());
        }
        return result;
    }

    private static String parseSection(String section, String targetCode, String description, String key, Map<String, String> result) {
        String nextSection = "";
        int index = -1;
        while ((index = section.indexOf(targetCode, index + 1)) != -1) {
            log("找到'" + targetCode + "'在位置: " + index);
            log(section.substring(index, index + 8));
            String dateStr = section.substring(index + 2, index + 8);
            if (isValidDate(dateStr, key, result)) {
                log(description + ": " + dateStr);
                nextSection = section.substring(0, index) + section.substring(index + 8);
                break;
            }
        }
        return nextSection;
    }

    private static boolean isValidDate(String dateStr, String key, Map<String, String> result) {
        return isNumeric(dateStr) && isDate(dateStr, key, result);
    }

    public static Boolean isNumeric(String str) {
        boolean isNumeric = true;
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                isNumeric = false;
                break;
            }
        }
        return isNumeric;
    }

    public static Boolean isDate(String str, String key, Map<String, String> result) {
        boolean isDate = true;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd"); // 假设是日/月/年的两位数年份格式
        try {
            LocalDate date = LocalDate.parse(str, formatter);
            result.put(key, date.toString());
            log("转换后的日期: " + date);
        } catch (DateTimeException e) {
            isDate = false;
            log("转换失败: " + e.getMessage());
        }
        return isDate;
    }

    public static void decomposePI(String nextPI, Map<String, String> result) {
        try {
            if (nextPI.startsWith(BATCH_PREFIX)) {
                processPrefix(SERIAL_PREFIX, "批号", "序列号", nextPI, "batch", "serial", result);
            } else if (nextPI.startsWith(SERIAL_PREFIX)) {
                processPrefix(BATCH_PREFIX, "序列号", "批号", nextPI, "serial", "batch", result);
            } else {
                log("无法识别的格式: " + nextPI);
            }
        } catch (Exception e) {
            log("解析过程中发生异常: " + e.getMessage());
        }
    }

    private static void processPrefix(String prefix, String batchKey, String serialKey, String nextPI, String key, String nextKey, Map<String, String> result) {
        int firstIndex = nextPI.indexOf(prefix);
        if (firstIndex != -1) {
            String subString = nextPI.substring(2, firstIndex);
            log(batchKey + ": " + subString);
            result.put(key, subString);
            String nextString = nextPI.substring(firstIndex + 2);
            log(serialKey + ": " + nextString);
            result.put(nextKey, nextString);
        } else {
            String subString = nextPI.substring(2);
            log(batchKey + ": " + (subString.isEmpty() ? EMPTY_VALUE : subString));
            result.put(key, (subString.isEmpty() ? EMPTY_VALUE : subString));
            log(serialKey + ": " + EMPTY_VALUE);
            result.put(nextKey, EMPTY_VALUE);
        }
    }

    private static void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        String udiString = "";
        parseUdiString(udiString);
    }

}