package jedi.serialization;

import java.util.Date;

/**
 * User: zhaoyao
 * Date: 11-12-30
 * Time: 下午8:50
 */
public class DateStringSerializer implements StringSerializer<Date>{

    public String toString(Date date) {
        return String.valueOf(date.getTime() / 1000);
    }

    public Date fromString(String value) {
        return new Date(Long.parseLong(value) * 1000);
    }
}
