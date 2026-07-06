package org.apache.commons.logging;

/**
 * commons-logging'in yerine geçen mini arayüz. Gerçek kütüphanenin log
 * implementasyonu keşfi Android'de çöktüğü için predict4java'nın ihtiyaç
 * duyduğu API burada yeniden sağlanıyor (bkz. build.gradle.kts exclude).
 */
public interface Log {
    boolean isDebugEnabled();

    void debug(Object message);

    void debug(Object message, Throwable t);

    void info(Object message);

    void warn(Object message);

    void error(Object message);

    void error(Object message, Throwable t);
}
