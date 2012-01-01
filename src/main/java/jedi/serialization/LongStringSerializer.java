package jedi.serialization;

/**
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午8:57
 */
public class LongStringSerializer implements StringSerializer<Long>{

    @Override
    public String toString(Long object) {
        return object.toString();
    }

    @Override
    public Long fromString(String value) {
        return Long.parseLong(value);
    }
}
