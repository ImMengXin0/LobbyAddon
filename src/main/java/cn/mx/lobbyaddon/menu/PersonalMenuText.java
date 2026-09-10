package cn.mx.lobbyaddon.menu;

import java.util.function.Function;

final class PersonalMenuText {

    private PersonalMenuText() {
    }

    static String profileValue(String value, Function<String, String> renderer) {
        return value == null || value.isEmpty() ? "&8未设置" : renderer.apply(value);
    }

    static String profileValueOrDefault(String value, String fallback, Function<String, String> renderer) {
        return renderer.apply(value == null || value.isEmpty() ? fallback : value);
    }
}
