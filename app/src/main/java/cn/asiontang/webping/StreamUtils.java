package cn.asiontang.webping;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("unused")
public final class StreamUtils
{
    public static void closeQuietly(final Closeable closeable)
    {
        if (closeable == null)
            return;
        try
        {
            closeable.close();
        }
        catch (final Exception ignore)
        {
        }
    }

    /**
     * 将输入流的所有数据读取为Byte数组；默认会InputStream.close；关闭输入流。
     */
    public static byte[] readAllBytes(final InputStream inStream) throws IOException
    {
        return readAllBytes(inStream, true, null);
    }

    /**
     * 将输入流的所有数据读取为Byte数组；
     *
     * @param autoCloseInputStream 读取完毕后，是否自动调用InputStream.close；
     */
    public static byte[] readAllBytes(final InputStream inStream, final boolean autoCloseInputStream) throws IOException
    {
        return readAllBytes(inStream, true, null);
    }

    public static byte[] readAllBytes(final InputStream inStream, final boolean autoCloseInputStream//
            , final ProgressTickHandler progressTickHandler) throws IOException
    {
        int len;
        int totalLength = 0;
        int size = 1024;
        byte[] buf;
        ByteArrayOutputStream bos = null;
        try
        {
            if (inStream instanceof ByteArrayInputStream)
            {
                size = inStream.available();
                buf = new byte[size];
                len = inStream.read(buf, 0, size);

                //更新读取进度
                if (progressTickHandler != null)
                    progressTickHandler.setProgressValue(len);
            }
            else
            {
                bos = new ByteArrayOutputStream(size);
                buf = new byte[size];

                while ((len = inStream.read(buf, 0, size)) != -1)
                {
                    bos.write(buf, 0, len);

                    //更新读取进度
                    if (progressTickHandler != null)
                    {
                        totalLength += len;
                        progressTickHandler.setProgressValue(totalLength);
                    }
                }

                buf = bos.toByteArray();
            }
        }
        finally
        {
            //临时的内存流必须记得要关闭！
            QuietlyCloseUtils.close(bos);

            if (autoCloseInputStream)
                QuietlyCloseUtils.close(inStream);
        }
        return buf;
    }

    public static String readAllBytesAsString(final InputStream inStream, final String charsetName) throws IOException
    {
        return new String(readAllBytes(inStream), charsetName);
    }
}
