package bat.ke.qq.com.common.utils;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConvertUtil {
    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap();

    public ConvertUtil() {
    }

    private static <T> Schema<T> getSchema(Class<T> cls) {
        Schema<T> schema = (Schema)cachedSchema.get(cls);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(cls);
            if (schema != null) {
                cachedSchema.put(cls, schema);
            }
        }

        return (Schema)schema;
    }

    public static <T> byte[] serialize(T obj) {
        Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(512);

        byte[] var4;
        try {
            Schema<T> schema = getSchema(cls);
            var4 = ProtobufIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception var8) {
            throw new IllegalStateException(var8.getMessage(), var8);
        } finally {
            buffer.clear();
        }

        return var4;
    }

    public static <T> T unserialize(byte[] data, Class<T> cls) {
        try {
            T message = cls.newInstance();
            Schema<T> schema = getSchema(cls);
            ProtobufIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (Exception var4) {
            throw new IllegalStateException(var4.getMessage(), var4);
        }
    }

    public static <T> List<T> unserialize(List<byte[]> data, Class<T> clazz)
    {
        List<T> result = new ArrayList<T>();
        for (byte[] itemBytes : data)
        {
            result.add(unserialize(itemBytes, clazz));
        }
        return result;
    }
}
