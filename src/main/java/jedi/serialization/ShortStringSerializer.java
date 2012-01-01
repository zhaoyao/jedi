package jedi.serialization;

/**
 * User: zhaoyao
 * Date: 12-1-1
 */
public class ShortStringSerializer implements StringSerializer<Short>{
	@Override
	public String toString(Short object) {
		return object.toString();
	}

	@Override
	public Short fromString(String value) {
		return Short.parseShort(value);
	}
}
