package com.revature.repos;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.type.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.revature.dto.PageDTO;
import com.revature.models.Story;
import com.revature.models.Tag;
import com.revature.models.User;

@Repository
public class StoryRepo {

	@Autowired
	EntityManagerFactory emf;

	/**
	 * Creates id for story and saves the story to the database.
	 * @param newStory to save to the database
	 * @return the story that was saved to the database, or null if the story could
	 *         not be added to the database
	 */
	public Story saveStory(Story newStory) {
		SessionFactory sf = emf.unwrap(SessionFactory.class);
		try (Session session = sf.openSession()) {
			Transaction tx = session.beginTransaction();
			int id = (int) session.save(newStory);
			newStory.setId(id);
			tx.commit();
		}
		return newStory;
	}

	/**
	 * This merges the updated story to the database.
	 * @param currStory is the updated story to merge.
	 * @return the story that was merged into the database
	 */
	public Story editStory(Story currStory) {
		SessionFactory sf = emf.unwrap(SessionFactory.class);
		try (Session session = sf.openSession()) {
			Transaction tx = session.beginTransaction();
			Story updatedStory = (Story) session.merge(currStory);
			tx.commit();
			return updatedStory;
		}

	}

	/**
	 * Retrieves a story from the database using an id.
	 * @param id of the story to retrieve.
	 * @return the story retrieved from the database.
	 */
	public Story getStoryById(int id) {
		SessionFactory sf = emf.unwrap(SessionFactory.class);
		try (Session session = sf.openSession()) {
			Story story = session.get(Story.class, id);
			Hibernate.initialize(story.getChapters());
			Hibernate.initialize(story.getComments());
			return story;
		}
	}
	
	
	/**
	 * Gets a page of stories for the logged in user
	 * @param tokenValue to identify the logged in user
	 * @param pageInfo created page object to add the found stories into
	 * @return a page of stories
	 */
	public PageDTO<Story> getUserStories(User author, PageDTO<Story> pageInfo) {
	  SessionFactory sf = emf.unwrap((SessionFactory.class));
	  try (Session session = sf.openSession()) {
	    List<?> stories = session
	        .createQuery("select s from Story s where s.author.id = :id order by s.creationDate desc")
	        .setParameter("id", author.getId())
	        .list();
	    pageInfo.setResultCount(stories.size());

      // Create the page array for the stories
      List<Story> pageArray = new ArrayList<Story>();
      int i = pageInfo.getCurPage() * pageInfo.getPageSize();
      while(i < pageInfo.getResultCount() && i < (pageInfo.getCurPage() + 1) * pageInfo.getPageSize()) {
        pageArray.add((Story)stories.get(i));
        Hibernate.initialize(pageArray.get(i).getChapters());
        Hibernate.initialize(pageArray.get(i).getComments());
        i++;
      }
      pageInfo.setStories(pageArray);
      return pageInfo;
	  }
	}

	
	/**
	 * Get a page of stories from the database whose titles are similar to a given
	 * query.
	 * @param query    to compare the story title to
	 * @param pageInfo that describes the parameters of the page
	 * @return the page of the stories
	 */
	public PageDTO<Story> filterStoriesByQuery(String query, PageDTO<Story> pageInfo) {
		SessionFactory sf = emf.unwrap(SessionFactory.class);
		query = "%" + query + "%";
		try (Session session = sf.openSession()) {
			String fullQuery = "%" + query + "%";
			List<?> stories = session
					.createQuery("select s from Story s where lower(s.title) like lower(:query) order by s.creationDate desc")
					.setParameter("query", fullQuery, StringType.INSTANCE).list();
			pageInfo.setResultCount(stories.size());

			// Create the page array for the stories
			List<Story> pageArray = new ArrayList<Story>();
			int i = pageInfo.getCurPage() * pageInfo.getPageSize();
			while(i < pageInfo.getResultCount() && i < (pageInfo.getCurPage() + 1) * pageInfo.getPageSize()) {
				pageArray.add((Story)stories.get(i));
				Hibernate.initialize(pageArray.get(i).getChapters());
				Hibernate.initialize(pageArray.get(i).getComments());
				i++;
			}
			pageInfo.setStories(pageArray);
			return pageInfo;
		}
	}

	/**
	 * Get a page of stories from the database that are of a specific genre.
	 * @param genre    of the stories to get from the database
	 * @param pageInfo that describes the parameters of the page
	 * @return the page of the stories
	 */
	public PageDTO<Story> filterStoriesByGenre(String genre, PageDTO<Story> pageInfo) {
		SessionFactory sf = emf.unwrap(SessionFactory.class);
		try (Session session = sf.openSession()) {
			List<?> stories = session
					.createQuery("select s from Story s where s.genre = :genre order by s.creationDate desc")
					.setParameter("genre", genre, StringType.INSTANCE).list();
			pageInfo.setResultCount(stories.size());

			// Create the page array for the stories
			List<Story> pageArray = new ArrayList<Story>();
			int i = pageInfo.getCurPage() * pageInfo.getPageSize();
			while(i < pageInfo.getResultCount() && i < (pageInfo.getCurPage() + 1) * pageInfo.getPageSize()) {
				pageArray.add((Story)stories.get(i));
				Hibernate.initialize(pageArray.get(i).getChapters());
				Hibernate.initialize(pageArray.get(i).getComments());
				i++;
			}
			pageInfo.setStories(pageArray);
			return pageInfo;
		}
	}

	/**
	 * Get a page of stories from the database that have a specific tag.
	 * @param tag      to filter by
	 * @param pageInfo that describes the parameters of the page
	 * @return the page of the stories
	 */
	public PageDTO<Story> filterStoriesByTag(String tag, PageDTO<Story> pageInfo) {
		SessionFactory sf = emf.unwrap(SessionFactory.class);
		try (Session session = sf.openSession()) {
			Tag tagObject = (Tag) session.createQuery("select t from Tag t where t.name like :tag")
					.setParameter("tag", tag).uniqueResult();

			// Checks if the tag does not exist
			if (tagObject == null) {
				pageInfo.setStories(new ArrayList<Story>());
				return pageInfo;
			}
			
			// Get the sorted stories associated with the tag
			List<Story> stories = tagObject.getStories();
			stories.sort((a, b) -> Long.compare(a.getCreationDate(), b.getCreationDate()));
			pageInfo.setResultCount(stories.size());

			// Create the page array for the stories
			List<Story> pageArray = new ArrayList<Story>();
			int i = pageInfo.getCurPage() * pageInfo.getPageSize();
			while(i < pageInfo.getResultCount() && i < (pageInfo.getCurPage() + 1) * pageInfo.getPageSize()) {
				pageArray.add((Story)stories.get(i));
				Hibernate.initialize(pageArray.get(i).getChapters());
				Hibernate.initialize(pageArray.get(i).getComments());
				i++;
			}
			pageInfo.setStories(pageArray);
			return pageInfo;
		}
	}

	/**
	 * Get a page of stories from all stories in the database.
	 * @param pageInfo that describes the parameters of the page
	 * @return the page of the stories
	 */
	public PageDTO<Story> getAllStories(PageDTO<Story> pageInfo) {
		SessionFactory sf = emf.unwrap(SessionFactory.class);
		try (Session session = sf.openSession()) {
			// Get the stories from the database
			List<?> stories = session.createQuery("select s from Story s order by s.creationDate desc").list();
			pageInfo.setResultCount(stories.size());

			// Create the page array for the stories
			List<Story> pageArray = new ArrayList<Story>();
			int i = pageInfo.getCurPage() * pageInfo.getPageSize();
			while(i < pageInfo.getResultCount() && i < (pageInfo.getCurPage() + 1) * pageInfo.getPageSize()) {
				pageArray.add((Story)stories.get(i));
				Hibernate.initialize(pageArray.get(i).getChapters());
				Hibernate.initialize(pageArray.get(i).getComments());
				i++;
			}
			pageInfo.setStories(pageArray);
			return pageInfo;
		}
	}
}
