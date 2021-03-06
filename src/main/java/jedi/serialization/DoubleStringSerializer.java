package jedi.serialization;

/**
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午8:57
 */
public class DoubleStringSerializer implements StringSerializer<Double> {

    @Override
    public String toString(Double object) {
        return object.toString();
    }

    @Override
    public Double fromString(String value) {
        return Double.parseDouble(value);
    }

}
