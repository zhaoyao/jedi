package jedi;

import jedi.annotation.Id;
import jedi.annotation.Index;
import jedi.annotation.Indexes;

import java.util.Date;

/**
 * User: zhaoyao
 * Date: 11-12-31
 */
@Indexes(
		{
				@Index(on = "title", range = "publishedAt"),
				@Index(on = "title", range = "published"),
				@Index(on = "title, published", range = "publishedAt"),
				@Index(on = "published")
		}
)
public class Story {

	private Long id;
	private String title;
	private String subtitle;
	private boolean published;

	private Date publishedAt;

	private int duration;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public Date getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(Date publishedAt) {
		this.publishedAt = publishedAt;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}
}
