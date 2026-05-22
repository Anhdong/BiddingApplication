package com.uet.BiddingApplication.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SupabaseUtil {

    private static final Logger log = LoggerFactory.getLogger(SupabaseUtil.class);


    //Autoremove query (?token=...) để tránh lỗi regex.
    public static String getExtensionFromUrl(String supabaseUrl) {
        if (supabaseUrl == null || supabaseUrl.isEmpty()) {
            return "";
        }
        try {
            // Lấy phần path của URL để loại bỏ query parameters (?token=xyz)
            String path = URI.create(supabaseUrl).getPath();

            Pattern pattern = Pattern.compile("(\\.[^.]+)$");
            Matcher matcher = pattern.matcher(path);

            if (matcher.find()) {
                log.info("[SupabaseUtil] Lấy image extension thành công");
                return matcher.group(1).toLowerCase();
            }
        } catch (Exception e) {
            log.error("[SupabaseUtil] Lỗi khi phân tích URL để lấy extension: {}", e.getMessage());
        }
        return ""; // Trả về chuỗi rỗng nếu không tìm thấy
    }

    //Download Image byte[]
    public static byte[] downloadImageBytes(String supabaseUrl) {
        if (supabaseUrl == null || supabaseUrl.isEmpty()) {
            return null;
        }
        try (InputStream in = URI.create(supabaseUrl).toURL().openStream()) {
            log.info("[SupabaseUtil] Tải hình ảnh thành công");
            return in.readAllBytes();
        } catch (IOException e) {
            log.error("[SupabaseUtil] Lỗi khi tải ảnh từ Supabase: {}", e.getMessage());
            return null;
        }
    }
}