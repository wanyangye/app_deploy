package com.bc.app_deploy.model.enums;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

@Slf4j
@Getter
public enum LocaleEnum {
    zh_CN("zh_CN", "中文简体 (中国)"),
    zh_SG("zh_SG", "中文简体 (新加坡)"),
    en_US("en_US", "英语 (美国)"),
    zh_TW("zh_TW", "中文繁体 (台湾)"),
    ;

    private final String code;
    private final String desc;

    LocaleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static LocaleEnum from(String raw) {
        if (StringUtils.isBlank(raw)) {
            return zh_CN;
        }
        // 兼容 Accept-Language
        raw = raw.replace("-", "_");

        for (LocaleEnum locale : values()) {
            if (raw.startsWith(locale.code)) {
                return locale;
            }
        }
        log.warn("检测到系统不支持的语言枚举：{}，自动转换为zh_CN", raw);
        return zh_CN;
    }

    public static Locale ofLocale(String languageCode) {
        LocaleEnum localeEnum = from(languageCode);
        return switch (localeEnum) {
            case en_US -> Locale.US;
            case zh_TW -> Locale.TRADITIONAL_CHINESE;
            default -> Locale.SIMPLIFIED_CHINESE;
        };
    }
}
