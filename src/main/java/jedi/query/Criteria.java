package jedi.query;

/**
 * User: zhaoyao
 * Date: 12-1-1
 */
public class Criteria {
	
	private String property;
	private Object value;

	public Criteria(String property, Object value) {
		this.property = property;
		this.value = value;
	}

	public String getProperty() {
		return property;
	}

	public Object getValue() {
		return value;
	}
}
