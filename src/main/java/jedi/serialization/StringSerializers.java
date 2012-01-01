package jedi.serialization;

import jedi.JediException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午8:58
 */
@SuppressWarnings("unchecked")
public class StringSerializers {

	private static final Map<Class<?>, StringSerializer<?>> DEFAULT_SERIALIZERS = new HashMap<Class<?>, StringSerializer<?>>();

	static {
		DEFAULT_SERIALIZERS.put(Date.class, new DateStringSerializer());
		DEFAULT_SERIALIZERS.put(Long.class, new LongStringSerializer());
		DEFAULT_SERIALIZERS.put(long.class, new LongStringSerializer());
		DEFAULT_SERIALIZERS.put(Integer.class, new IntegerStringSerializer());
		DEFAULT_SERIALIZERS.put(int.class, new IntegerStringSerializer());
		DEFAULT_SERIALIZERS.put(Double.class, new DoubleStringSerializer());
		DEFAULT_SERIALIZERS.put(double.class, new DoubleStringSerializer());
		DEFAULT_SERIALIZERS.put(Float.class, new FloatStringSerializer());
		DEFAULT_SERIALIZERS.put(float.class, new FloatStringSerializer());
		DEFAULT_SERIALIZERS.put(Boolean.class, new BooleanStringSerializer());
		DEFAULT_SERIALIZERS.put(boolean.class, new BooleanStringSerializer());

	}

	public static void register(Class<?> type, StringSerializer<?> serializer) {
		DEFAULT_SERIALIZERS.put(type, serializer);
	}


	public static <T> StringSerializer<T> lookup(Class<T> type) {
		return (StringSerializer<T>) DEFAULT_SERIALIZERS.get(type);
	}

	public static String toString(Object o) {
		if (o == null) {
			return null;
		}

		if (o instanceof String) {
			return (String) o;
		}

		StringSerializer serializer = lookup(o.getClass());
		if (serializer == null) {
			throw new JediException("Can not find StringSerializer for type: " + o.getClass()
					+ ", register the StringSerializer using StringSerializers.register(Class<?> type, StringSerializer<?> serializer)");
		}

		return serializer.toString(o);
	}

	public static <T> T fromString(String o, Class<T> type) {
		if (type == String.class) {
			return (T) o;
		}

		if (o == null) {
			return null;
		}

		StringSerializer<T> serializer = lookup(type);
		return serializer.fromString(o);
	}

}
