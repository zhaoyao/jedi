package jedi.serialization;

/**
 * Created by IntelliJ IDEA.
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午8:52
 */
public interface StringSerializer<T> {

    String toString(T object);

    T fromString(String value);
}
