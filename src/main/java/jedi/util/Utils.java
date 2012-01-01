package jedi.util;

import com.google.common.base.Preconditions;
import jedi.annotation.Id;
import jedi.serialization.StringSerializers;
import jodd.bean.BeanTool;
import jodd.bean.BeanUtil;
import jodd.introspector.ClassDescriptor;
import jodd.introspector.ClassIntrospector;
import jodd.util.ArraysUtil;
import jodd.util.StringUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * User: zhaoyao
 * Date: 11-12-31
 */
public class Utils {

	public static Map<String, String> serialize(Object o) {
		Map<String, Object> fields = new HashMap<String, Object>();
		BeanTool.copy(o, fields, false, false);



		return stringrify(fields);
	}

	public static Map<String, String> stringrify(Map<String, Object> attributes) {
		Map<String, String> result = new HashMap<String, String>(attributes.size());

		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			String string = StringSerializers.toString(entry.getValue());
			result.put(entry.getKey(), string);
		}

		return result;
	}

	public static String[] stringrify(Object[] objects) {
		String[] result = new String[objects.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = StringSerializers.toString(objects[i]);
		}
		return result;
	}

	public static <T> T instantiate(Class<T> type) {
		try {
			return type.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * todo: remove properties
	 *
	 * @param type
	 * @return
	 */
	public static String[] getAttributeNames(Class<?> type) {
		ClassDescriptor classDescriptor = ClassIntrospector.lookup(type);
		String[] names = classDescriptor.getAllBeanGetterNames();
		Arrays.sort(names);
		return names;
	}


	public static <T> T deserialize(Map<String, String> attributes, Class<T> type) {

		T result = instantiate(type);

		for (Map.Entry<String, String> entry : attributes.entrySet()) {
			String name = entry.getKey();
			Class<?> propertyType = BeanUtil.getPropertyType(result, name);

			BeanUtil.setSimpleProperty(
					result,
					name, StringSerializers.fromString(entry.getValue(), propertyType),
					true);

		}
		return result;
	}

	public static String getIdPropertyName(Object o) {
		return getIdPropertyName(o.getClass());
	}

	public static String getIdPropertyName(Class<?> type) {
		Preconditions.checkNotNull(type);

		ClassDescriptor classDescriptor = ClassIntrospector.lookup(type);
		Method[] getters = classDescriptor.getAllBeanGetters();
		for (Method getter : getters) {
			if (getter.isAnnotationPresent(Id.class)) {
				return StringUtil.camelCaseToWords(getter.getName()).split(" ")[1];
			}
		}

		Field[] fields = classDescriptor.getAllFields(true);
		for (Field field : fields) {
			if (field.isAnnotationPresent(Id.class)) {
				return field.getName();
			}
		}

		throw new IllegalStateException("@Id not exists in Class: " + type);
	}

	public static Object getIdValue(Object object) {
		return BeanUtil.getSimpleProperty(object, getIdPropertyName(object), true);
	}

	public static final Class[] SUPPORTED_ID_TYPES = {int.class, Integer.class, long.class, Long.class};


	public static Class<?> getIdPropertyType(Class<?> type) {
		ClassDescriptor classDescriptor = ClassIntrospector.lookup(type);
		Class<?> idPropertyType = classDescriptor.getBeanGetter(getIdPropertyName(type)).getReturnType();

		if (!ArraysUtil.contains(SUPPORTED_ID_TYPES, idPropertyType)) {
			throw new IllegalStateException("Id type of class: " + type + " is " + idPropertyType + " which is not allowed");
		}

		return idPropertyType;
	}



}
