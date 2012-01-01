package jedi.serialization;

/**
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午8:55
 */
public class IntegerStringSerializer implements StringSerializer<Integer> {

    @Override
    public String toString(Integer object) {
        return object.toString();
    }

    @Override
    public Integer fromString(String value) {
        return Integer.parseInt(value);
    }
}
