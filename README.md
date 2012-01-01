#Jedi
Jedi is a simple and small library for storing objects in redis. It uses [Jedis](http://github.com/xetorthio/jedis "Jedis") internally so it is fully compatible with Redis 2.0.0.

**This project is not ready for production yet**.

# It is not an orm framework
So it does not support orm features, like relation mappings, which [Johm](https://github.com/xetorthio/johm "Johm") provided.

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

###Basic CRUD

```java

//initialize jedis pool
JedisPool jedisPool = new JedisPool("localhost", 6379);
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

### Directly talk to redis

```java
jedi.withJedis(new JedisCallback() {
    public void execute(Jedis jedis) {
        //automatic jedis pool resource managment like JdbcTemplate does
    }

});
```
or

```java
jedi.withTransaction(new TransactionCallback() {
    public void execute(Transaction transaction) {
        //automatic transaltion start, commit and discard on exception
    }
});
```


###Index support
Let's add some indexes to Book

```java
@Index(on = "author")
public class Book {
   // 
}
```
Then we can find books by their author

```java
Iterator<Book> booksOfRowling = jedis.find(Book.class).where("author").is("J.K.Rowling").iterate(0, 1);
```
index rebuild is still in working progress. So for now, the **old objects will not be indexed**.

###Range support

```java
@Index(on = "author", range = "publishedAt")
public class Book {
    //
}
```
Then you can find books by author and its published date

```java
Iterator<Book> booksOfRowlingAfter2001 = jedis.find(Book.class)
                                            .where("author")
                                                .is("J.K.Rowling")
                                            .andRange("publishedAt")
                                                .within(date2001InEpoch, Double.MAX_VALUE);
```
If you need to index author both in published date range and some other fields, you should declare it like: 

```java
@Indexes(
    {
        @Index(on = "author", range = "publishedAt"),
        @Index(on = "author", range = "someOtherField")
    }
)
public class Book {
    //
}
```
###Multi fields index support
So what to do if we want to find books by its author and its title?

```java
@Index(on = {"author", "title"})
//or you may prefer @Index(on = "author, title")
public class Book {
    //
}
```
Then you can find them like: 

```java
jedi.find(Book.class).where("author").is("J.K.Rowling").and("title").is("Harry Potter").iterator(0, 1);
```
Maybe we should add some method like _fetchOne_ for unique index?
#Serialization
The java object will be converted to String based hash, then perisist to redis.
for now jedi supports types:

* All basic primitive types and their wrappers
* java.util.Date
* String of couse

You can implemated your own StringSerializer, and register it using:

```java
StringSerializers.regiser(yourType, yourSerializer);
```

