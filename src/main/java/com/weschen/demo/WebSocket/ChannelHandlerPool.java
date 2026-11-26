package com.weschen.demo.WebSocket;

import com.weschen.demo.constant.WebSocketConstants;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.*;
import java.util.stream.Collectors;

public class ChannelHandlerPool {

    /**
     * 频道池
     */
    public ChannelHandlerPool() {
    }

    /**
     * 频道池
     */
    public static Set<Channel> channelGroup = Collections.synchronizedSet(new HashSet<>());

    /**
     * 按 channel 查找连接。
     * 用 channelId.equals(属性)，属性为 null 时得到 false，不会空指针。
     * 属性是空字符串时仍能匹配 channelId 也是空字符串的消息。
     * synchronizedSet 的迭代必须持有它自己这把锁，否则并发增删会抛 ConcurrentModificationException。
     */
    public static List<Channel> findByChannelId(String channelId) {
        synchronized (channelGroup) {
            if (channelId == null) {
                return Collections.emptyList();
            }
            AttributeKey<String> key = AttributeKey.valueOf(WebSocketConstants.CHANNEL_CHANNEL_ID_KEY);
            return channelGroup.stream()
                    .filter(channel -> channelId.equals(channel.attr(key).get()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 池快照。按 JetCache 统计表的方式对齐：列宽取表头和内容的最大字符数，分隔线由表头生成。
     */
    public static String dumpPool() {
        synchronized (channelGroup) {
            AttributeKey<String> channelKey = AttributeKey.valueOf(WebSocketConstants.CHANNEL_CHANNEL_ID_KEY);
            AttributeKey<String> langKey = AttributeKey.valueOf(WebSocketConstants.CHANNEL_LANG_KEY);
            List<String[]> rows = new ArrayList<>();
            for (Channel channel : channelGroup) {
                rows.add(new String[]{
                        cell(channel.remoteAddress()),
                        cell(channel.attr(channelKey).get()),
                        cell(channel.attr(langKey).get())
                });
            }
            // 排序后快照稳定，成员没变时不会因为 HashSet 遍历顺序不同而重复打日志
            rows.sort(Comparator.comparing((String[] row) -> row[0]).thenComparing(row -> row[1]).thenComparing(row -> row[2]));
            return formatTable(new String[]{"remote", "channel", "lang"}, rows);
        }
    }

    /**
     * 池统计：按 channel 分组的订阅数。空 channel 记为 -。
     */
    public static String poolStats() {
        synchronized (channelGroup) {
            AttributeKey<String> key = AttributeKey.valueOf(WebSocketConstants.CHANNEL_CHANNEL_ID_KEY);
            return channelGroup.stream()
                    .collect(Collectors.groupingBy(
                            ch -> cell(ch.attr(key).get()),
                            TreeMap::new,
                            Collectors.counting()))
                    .entrySet().stream()
                    .map(e -> e.getKey() + " × " + e.getValue())
                    .collect(Collectors.joining(", ", "[", "]"));
        }
    }

    /** null 和空字符串都显示成 -，避免表格里出现空白单元格。 */
    private static String cell(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value);
        return text.isEmpty() ? "-" : text;
    }

    /**
     * 对齐方式与 JetCache 统计表相同：宽度按字符数，用 String.format 左对齐。
     * 先打表头，再按表头生成分隔线（| 换成 +，其余换成 -），这样分隔线一定和列对齐。
     */
    private static String formatTable(String[] headers, List<String[]> rows) {
        int[] width = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            width[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < headers.length; i++) {
                width[i] = Math.max(width[i], row[i].length());
            }
        }
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            if (i > 0) {
                pattern.append('|');
            }
            pattern.append("%-").append(width[i]).append('s');
        }
        String headerLine = String.format(pattern.toString(), (Object[]) headers);
        StringBuilder sb = new StringBuilder();
        sb.append(headerLine).append('\n');
        appendSeparator(sb, headerLine);
        if (rows.isEmpty()) {
            sb.append("(无连接)\n");
        } else {
            for (String[] row : rows) {
                sb.append(String.format(pattern.toString(), (Object[]) row)).append('\n');
            }
        }
        appendSeparator(sb, headerLine);
        return sb.toString();
    }

    private static void appendSeparator(StringBuilder sb, String headerLine) {
        headerLine.chars().forEach(c -> sb.append(c == '|' ? '+' : '-'));
        sb.append('\n');
    }

}
