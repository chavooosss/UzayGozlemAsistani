package org.apache.commons.logging;

/** Android Log'a yönlendiren mini LogFactory (gerçek kütüphanenin yerine). */
public final class LogFactory {

    private LogFactory() {
    }

    public static Log getLog(Class<?> clazz) {
        final String tag = clazz.getSimpleName();
        return new Log() {
            @Override
            public boolean isDebugEnabled() {
                return false;
            }

            @Override
            public void debug(Object message) {
                android.util.Log.d(tag, String.valueOf(message));
            }

            @Override
            public void debug(Object message, Throwable t) {
                android.util.Log.d(tag, String.valueOf(message), t);
            }

            @Override
            public void info(Object message) {
                android.util.Log.i(tag, String.valueOf(message));
            }

            @Override
            public void warn(Object message) {
                android.util.Log.w(tag, String.valueOf(message));
            }

            @Override
            public void error(Object message) {
                android.util.Log.e(tag, String.valueOf(message));
            }

            @Override
            public void error(Object message, Throwable t) {
                android.util.Log.e(tag, String.valueOf(message), t);
            }
        };
    }
}
