package jedi.serialization;

/**
 * User: zhaoyao
 * Date: 11-12-31
 */
public class BooleanStringSerializer implements StringSerializer<Boolean>{
	@Override
	public String toString(Boolean object) {
		return object ? "1" : "0";
	}

	@Override
	public Boolean fromString(String value) {
		return value.equals("1");
	}
}
