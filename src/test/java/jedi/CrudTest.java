package jedi;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 * User: zhaoyao
 * Date: 12-1-1
 */
public class CrudTest extends BaseTest {

	@Test
	public void save_all_non_null_attributes_to_redis() {

		Story story = new Story();
		story.setTitle("I am delicious");
		jedi.save(story);
		assertThat(
				hashField(Story.class, story.getId(), "title"),
				equalTo(story.getTitle())
		);

		assertThat(
				hashField(Story.class, story.getId(), "subtitle"),
				isNull()
		);
	}

	@Test
	public void after_update_delete_fields_does_not_exists_anymore() {
		Story story = new Story();
		story.setTitle("I am delicious");
		story.setSubtitle("I am delicious too");
		jedi.save(story);

		assertThat(
				hashField(Story.class, story.getId(), "subtitle"),
				equalTo(story.getSubtitle())
		);

		story.setSubtitle(null);
		jedi.update(story);

		assertThat(
				hashField(Story.class, story.getId(), "subtitle"),
				isNull()
		);
	}

	@Test(expected = JediException.class)
	public void update_new_object() {
		Story story = new Story();
		story.setTitle("I am delicious");
		story.setSubtitle("I am delicious too");

		jedi.update(story);
	}

	@Test
	public void delete_object() {
		Story story = new Story();
		story.setTitle("I am delicious");
		story.setSubtitle("I am delicious too");
		jedi.save(story);

		assertThat(jedi.get(Story.class, story.getId()), isNotNull());

		jedi.delete(story);

		assertThat(jedi.get(Story.class, story.getId()), isNull());
	}

	@Test(expected = JediException.class)
	public void delete_new_object() {
		Story story = new Story();
		story.setTitle("I am delicious");
		story.setSubtitle("I am delicious too");

		jedi.delete(story);
	}


	private String hashField(Class type, long id, String name) {
		return getJedis().hmget(objectKey(type, id), name).get(0);
	}

	private String objectKey(Class type, long id) {
		return type.getName() + ":" + id;
	}


}
