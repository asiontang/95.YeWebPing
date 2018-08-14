package cn.asiontang.webping;

import java.io.Closeable;
import java.io.IOException;

/**
 * Unconditionally close a <code>Closeable</code>.
 * <p>
 * Equivalent to {@link Closeable#close()}, except any exceptions will be
 * ignored. This is typically used in finally blocks.
 * <p>
 * Example code:
 *
 * <pre>
 * Closeable closeable = null;
 * try
 * {
 *     closeable = new FileReader(&quot;foo.txt&quot;);
 *     // process closeable
 *     closeable.close();
 * }
 * catch (Exception e)
 * {
 *     // error handling
 * }
 * finally
 * {
 *     QuietlyCloseUtils.close(closeable);
 * }
 * </pre>
 */
public final class QuietlyCloseUtils
{
    /**
     * 这关闭一个流的方法将忽略nulls和IOException的情况。 包括对各种类型输入输出流的关闭。 无条件的关闭流。
     */
    public static final void close(final Closeable closeable)
    {
        if (closeable == null)
            return;
        try
        {
            closeable.close();
        }
        catch (final IOException ioe)
        {
            // ignore
        }
    }
}
