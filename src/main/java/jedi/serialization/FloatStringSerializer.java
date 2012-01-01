package jedi.serialization;

/**
 * Created by IntelliJ IDEA.
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午8:56
 */
public class FloatStringSerializer implements StringSerializer<Float>{

    @Override
    public String toString(Float object) {
        return object.toString();
    }

    @Override
    public Float fromString(String value) {
        return Float.parseFloat(value);
    }
}
