#Jedi
Jedi is a simple and small library for storing objects in redis. It uses [Jedis](http://github.com/xetorthio/jedis "Jedis") internally so it is fully compatible with Redis 2.0.0.

#Examples

Jedi works fine with simple java pojos, just use very small amount of annotations
```java
public class Book {
    @Id
    private Long id;
    private String author;
    private String title;
    private Date publishedAt;
}
```

Done. Let's begin some crud staffs
```java

//initialize jedis pool
JedisPool jedisPool = new JedisPool();
Jedi jedi = new Jedi(jedisPool);

Book book = new Book();
book.setTile("Harry Potter");
book.setAutor("J.K.Rowling");
jedi.save(book);

// the generated redis hash will be { id: __auto_generated__, title: "Harry Potter", author: "J.K.Rowling" }

assertNotNull(book.getId()); //auto id generation
assertNotNull(jedi.get(Book.class, book.getId())); //ready for id lookup

Long id = book.getId();

//oh, we forgot to set the publish date
book.setPublishedAt(new Date());
jedi.update(book);

// the redis hash will be updated to { id: __auto_generated__, title: "Harry Potter", author: "J.K.Rowling", publishedAt: __epoch__ }
// the java.util.Date will serialized to epoch, the Serialization will be covered later

jedi.delete(book); //see-ya, or you may need jedi.delete(Book.class, book.getId());

```

