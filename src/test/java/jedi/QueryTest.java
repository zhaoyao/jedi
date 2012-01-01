package jedi;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

/**
 * User: zhaoyao
 * Date: 12-1-1
 */
public class QueryTest extends BaseTest {

	@Test
	public void test() {

		Story story1 = new Story();
		story1.setTitle("I am delicious");
		story1.setSubtitle("Subtitle");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 2011);
		calendar.set(Calendar.MONTH, 11);
		calendar.set(Calendar.DAY_OF_MONTH, 31);
		story1.setPublishedAt(calendar.getTime());
		jedi.save(story1);

		Story story2 = new Story();
		story2.setTitle("I am delicious");
		story2.setSubtitle("Subtitle");
		Date publishedAt = new Date();
		story2.setPublishedAt(publishedAt);
		jedi.save(story2);

		Story story3 = new Story();
		story3.setTitle("I am delicious");
		story3.setSubtitle("Subtitle");
		story3.setPublishedAt(publishedAt);
		jedi.save(story3);

		Iterator<Story> iterator = jedi.find(Story.class).where("title").is("I am delicious")
				.andRange("publishedAt").within(publishedAt.getTime() - 100, Double.MAX_VALUE);

		while (iterator.hasNext()) {
			assertFalse(iterator.next().getId().equals(story1.getId()));
		}

		assertEquals(3, jedi.find(Story.class).where("title").is("I am delicious").count());
	}


}
