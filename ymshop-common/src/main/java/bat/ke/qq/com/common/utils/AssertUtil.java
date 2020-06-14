package bat.ke.qq.com.common.utils;

import bat.ke.qq.com.common.exception.YmshopException;
import org.springframework.util.StringUtils;

public class AssertUtil {
    private static final String EMAIL_REGEX = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
    private static final String MOBILE_REGEX = "^1\\d{10}$";

    public AssertUtil() {
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new YmshopException(message);
        } else if (object instanceof String && "".equals(object)) {
            throw new YmshopException(message);
        }
    }

    public static void notNull(Object object) {
        notNull(object, "[Assertion failed] - this argument is required; it must not be null");
    }

    public static void email(String str, String message) {
        if (!StringUtils.isEmpty(str)) {
            if (!str.matches("^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$")) {
                throw new YmshopException(message);
            }
        }
    }

    public static void mobile(String str, String message) {
        if (!StringUtils.isEmpty(str)) {
            if (!str.matches("^1\\d{10}$")) {
                throw new YmshopException(message);
            }
        }
    }
}
