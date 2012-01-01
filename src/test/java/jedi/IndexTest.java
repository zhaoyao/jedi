package jedi;

import com.google.common.base.Joiner;
import jedi.serialization.StringSerializers;
import org.junit.Test;

import java.util.Date;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;

/**
 * User: zhaoyao
 * Date: 11-12-31
 */
public class IndexTest extends BaseTest {

	@Test
	public void insert_all_indexed_attributes_into_index() {
		Story story = new Story();
		story.setTitle("I am delicious");
		jedi.save(story);

		assertThat(index(Story.class, null, "published", "0"), equalToIndex(story.getId()));
	}

	@Test
	public void inserts_multiple_objects_into_the_same_index() {
		Story story1 = new Story();
		story1.setTitle("I am delicious");
		jedi.save(story1);

		Story story2 = new Story();
		story2.setTitle("I am delicious");
		jedi.save(story2);

		assertThat(
				index(Story.class, "publishedAt", "title", story1.getTitle()),
				equalToIndex(story1.getId(), story2.getId())
		);
	}

	@Test
	public void does_not_insert_non_indexed_attributes() {
		Story story = new Story();
		story.setTitle("I am delicious");
		story.setSubtitle("Subtitle");
		jedi.save(story);

		assertThat(
				index(Story.class, null, "subtitle", story.getSubtitle()),
				isEmpty()
		);
	}

	@Test
	public void indexes_on_combinations_of_attributes() {
		Story story = new Story();
		story.setTitle("I am delicious");
		story.setSubtitle("Subtitle");
		jedi.save(story);
		assertThat(
				index(Story.class, "publishedAt", "title", story.getTitle(), "published", StringSerializers.toString(story.isPublished())),
				equalToIndex(story.getId())
		);
	}

	@Test
	public void when_the_value_is_null_does_not_insert_to_the_index() {
		Story story = new Story();
		story.setSubtitle("Subtitle");
		jedi.save(story);

		assertThat(
				index(Story.class, "publishedAt", "title", ""),
				isEmpty()
		);
	}

	@Test
	public void after_update_removes_from_the_affected_index() {
		Story story = new Story();
		story.setTitle("I am delicious");
		story.setSubtitle("Subtitle");
		jedi.save(story);

		assertThat(
				index(Story.class, "publishedAt", "title", story.getTitle()),
				equalToIndex(story.getId())
		);

		story.setTitle("I am fabulous");
		jedi.update(story);

		assertThat(
				index(Story.class, "publishedAt", "title", "I am delicious"),
				isEmpty()
		);
	}

	@Test
	public void after_update_the_index_with_range_property_should_all_be_updated() {
		Story story = new Story();
		story.setTitle("I am delicious");
		story.setSubtitle("Subtitle");
		Date publishedAt = new Date();
		story.setPublishedAt(publishedAt);
		jedi.save(story);

		Double zscore1 = getJedis().zscore(indexKey(Story.class, "publishedAt", new String[]{"title"}, new String[]{story.getTitle()}), story.getId().toString());
		Double zscore2 = getJedis().zscore(indexKey(Story.class, "publishedAt", new String[]{"title", "published"}, new String[]{story.getTitle(), "0"}), story.getId().toString());
		assertThat(
				zscore1.toString(),
				//double to string -> 1.325411323E9
				equalTo(
						Double.toString(Double.parseDouble(StringSerializers.toString(publishedAt)))
				)
		);
		assertThat(
				zscore2.toString(),
				//double to string -> 1.325411323E9
				equalTo(
						Double.toString(Double.parseDouble(StringSerializers.toString(publishedAt)))
				)
		);

		publishedAt = new Date(System.currentTimeMillis() - 1000000);
		story.setPublishedAt(publishedAt);
		jedi.update(story);

		zscore1 = getJedis().zscore(indexKey(Story.class, "publishedAt", new String[]{"title"}, new String[]{story.getTitle()}), story.getId().toString());
		zscore2 = getJedis().zscore(indexKey(Story.class, "publishedAt", new String[]{"title", "published"}, new String[]{story.getTitle(), "0"}), story.getId().toString());
		assertThat(
				zscore1.toString(),
				//double to string -> 1.325411323E9
				equalTo(
						Double.toString(Double.parseDouble(StringSerializers.toString(publishedAt)))
				)
		);
		assertThat(
				zscore2.toString(),
				//double to string -> 1.325411323E9
				equalTo(
						Double.toString(Double.parseDouble(StringSerializers.toString(publishedAt)))
				)
		);
	}

	@Test
	public void after_delete_removes_from_the_the_cache_on_keys_matching_the_original_values_of_attributes() {
		Story story = new Story();
		story.setTitle("I am delicious");
		story.setSubtitle("Subtitle");
		jedi.save(story);
		assertThat(
				index(Story.class, "publishedAt", "title", story.getTitle()),
				equalToIndex(story.getId())
		);

		jedi.delete(Story.class, story.getId());
		assertThat(
				index(Story.class, "publishedAt", "title", story.getTitle()),
				isEmpty()
		);
	}

	@Test
	public void after_delete_only_removes_one_item_from_index_not_all_of_them() {
		Story story1 = new Story();
		story1.setTitle("I am delicious");
		story1.setSubtitle("Subtitle");
		jedi.save(story1);

		Story story2 = new Story();
		story2.setTitle("I am delicious");
		story2.setSubtitle("Subtitle");
		jedi.save(story2);

		assertThat(
				index(Story.class, "publishedAt", "title", story1.getTitle()),
				equalToIndex(story1.getId(), story2.getId())
		);

		jedi.delete(Story.class, story1.getId());

		assertThat(
				index(Story.class, "publishedAt", "title", story1.getTitle()),
				equalToIndex(story2.getId())
		);
	}

	private Set<String> index(Class type, String rangeProperty, String... kvs) {
		String[] keys = new String[kvs.length / 2];
		String[] values = new String[kvs.length / 2];
		for (int i = 0; i < kvs.length; i++) {
			String kv = kvs[i];
			if (i % 2 == 0) {
				keys[i / 2] = kv;
			} else {
				values[i / 2] = kv;
			}
		}

		String key = indexKey(type, rangeProperty, keys, values);

		return getJedis().zrange(key, 0, -1);
	}

	private String indexKey(Class type, String rangeProperty, String[] keys, String[] values) {
		if (rangeProperty != null) {
			rangeProperty += ":";
		} else {
			rangeProperty = "";
		}
		return type.getName() + ":" + Joiner.on(":").join(keys) + ":" + rangeProperty + Joiner.on(":").join(values);
	}

}
