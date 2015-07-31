SlingerORM
==========

SlingerORM is a simple Object Relation Mapper (ORM) focusing on speed and simplicity. It uses code
generation to generate code against the database that you don't want to write over and over again.

Basic usage
----------

The bare minimum that is needed for SlingerORM to work is an entity and an interface written by you:

    @DatabaseEntity
    public class ExampleEntity {
        @PrimaryKey
        private String id;
        private String name;

        public void setId(String id) {
          this.id = id;
        }

        public String getId() {
          return id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @DatabaseStorage
    public interface ExampleStorage {
        @CreateTable(ExampleEntity.class)
        void createTable(SQLiteDatabase db);

        @Insert
        void insert(SQLiteDatabase db, ExampleEntity entity);

        @Delete
        void delete(SQLiteDatabase db, ExampleEntity entity);

        @Select
        List<ExampleEntity> getAllExamples();
    }

Get an instance of your interface by accessing the generated builder by prefixing your interface
name with "Slinger":

    SQLiteDatabase db = ...
    ExampleStorage storage = SlingerExampleStorage.builder().build();

    storage.createTable(db);

    ExampleEntity entity = new ExampleEntity()
    entity.setId(42);
    entity.setName("David");
    storage.insert(db, entity);

    List<ExampleEntity> examples = storage.getAllExamples();
    ...


What does it really solve?
--------------------------

When writing your code for your database layer in Android you will have a lot of code like this:

Insert:

    SQLiteDatabase db = ...

    ExampleEntity item = ...
    ContentValues values = new ContentValues();
    values.put("id", item.getId());
    values.put("name", item.getName());
    db.insertOrThrow("ExampleEntity", null, values);

Query:

    Cursor cursor = null;
    try {
        cursor = db.query(false, "ExampleEntity", new String[] {
            "id",
            "name"
        }, "_id = ?", new String[] {
            String.valueOf(id)
        }, null, null, null, "1");

        if(!cursor.moveToFirst()) return null;

        ExampleEntity readItem = new ExampleEntity();
        readItem.setId(cursor.getLong(0));
        readItem.setName(cursor.getString(1));
        return readItem;
    } finally {
        if(cursor != null) cursor.close();
    }

Writing that becomes tedious when having over hundred fields in your database table that you want to
have mapped. You will write the wrong field name or having to add even more code to put those into
constants to be sure every field is mapped right. SlingerORM solves this problem by looking at
annotations for your class that represents your database table and generate the code above for you!

SlingerORM generates code using two annotation processors. The first generates a mapper class
that you can use to map the tedious part in the example above:

Insert:

    Mapper<ExampleEntity> mapper = new ExampleEntityMapper()
    db.insertOrThrow(mapper.getTableName(), null, mapper.mapValues(item));

Query:

    Mapper<ExampleEntity> mapper = new ExampleEntityMapper()
    Cursor cursor = null;
    try {
        cursor = db.query(false, mapper.getTableName(), mapper.getFieldNames(), "_id = ?",
                new String[] {
            String.valueOf(id)
        }, null, null, null, "1");
        if(!cursor.moveToFirst()) return null;

        return mapper.mapItem(cursor);
    } finally {
        if(cursor != null) cursor.close();
    }

The second annotation processor generates code for inserting, updating, deleting, querying etc
depending on what methods you provide in your custom storage interface (as seen in the basic usage
above)

Configuring the DatabaseEntity
------------------------------

SlingerORM will first look after getters and setters that match the fields (i.e setId,getId etc).
If it can't find any it getters or setters, it will try to access the fields directly. If that's not
possible the compiler will show an error that explains this.

If you have fields that doesn't match your corresponding set and get methods. You can annotate your
set and get methods with an annotation telling SlingerORM which field the method will set or get:

    @DatabaseEntity
    public class ExampleEntity {
      @PrimaryKey
      private String mId;
      private String mName;
      
      @SetField("mId")
      public void setId(String id) {
        mId = id;
      }
      
      @GetField("mId")
      public String getId() {
        return mId;
      }
      
      @SetField("mName")
      public void setName(String name) {
        mName = name;
      }
      
      @GetField("mName")
      public void getName() {
        return mName;
      }
    }

If you don't want to have the names of the field in your class as the columns in your table, you
can annotate these fields with @FieldName:

    @DatabaseEntity
    public class ExampleEntity {
      @FieldName("Id") @PrimaryKey
      private String id;
      @FieldName("Name")
      private String name;

      ...
    }

Using the mapper standalone
---------------------------

It's possible to use the database entity mapper and then do the quering etc yourself. You get an
implementation of the Mapper interface by suffixing the database entity name with "Mapper":

    Mapper<ExampleEntity> exampleEntityMapper = new ExampleEntityMapper();
    ...

Configuring the storage class
-----------------------------

The storage class uses one or more mappers that can be configured by calling methods in the builder.
The number of mappers used for a storage depends on how many different database entities are used
in the storage class:

    ExampleEntityStorage storage = SlingerExampleEntityStorage.builder()
        .exampleEntityMapper(new ExampleEntityMapper())
        .otherExampleMapper(new OtherExampleMapper())
        .build();
    ...

Here is some of the annotations that can be used on the methods in your storage interface:

    @DatabaseStorage
    public interface ExampleEntityStorage {
        //annotate method with this to create a table based on the given entity class
        @CreateTable(ExampleEntity.class)
        void createTable(SQLiteDatabase db);

        // slingerORM will insert the second parameter
        @Insert
        void insert(SQLiteDatabase db, ExampleEntity exampleEntity);

        // slingerORM will update the entity in the second parameter
        @Update
        void update(SQLiteDatabase db, ExampleEntity exampleEntity);

        //using an empty delete together with an "@DatabaseEntity" annotated class as parameter deletes the entity.
        @Delete
        void delete(SQLiteDatabase db, ExampleEntity exampleEntity);

        //you can also specify "where" to delete everything that matches the given query
        @Delete(where = "someVar = ?")
        void deleteItemsWithSomeVar(SQLiteDatabase db, String someVar);

        // slingerorm will match the "?" with your parameters, starting with the second from the left
        @Select(where = "_id = ?")
        ExampleEntity getEntity(SQLiteDatabase db, long id);

        // not setting any parameters on the annotation will cause it to use default and query all
        @Select
        List<ExampleEntity> getAll();

        // orderBy and limit can be set in the annotation which will be part of the sql query
        @Select(orderBy = "created DESC", limit = 5)
        List<ExampleEntity> getLatest(SQLiteDatabase db);
    }

Using a custom serializer
-------------------------

Sometimes some type of fields in the database entity will not be a native data type that SlingerORM
supports. You will then need to implement your custom serializer. Create a class and add
"@SerializeType" and "@DeserializeType" annotations to the methods. Deserialize methods will be
called when getting data from the database and serialize methods will be called when inserting data:

    public class MyCustomSerializer {
        @DeserializeType
        public Date deserializeDate(long time) {
            return new Date(time);
        }

        @SerializeType
        public long serializeDate(Date date) {
            return date.getTime();
        }
    }

To tell SlingerORM which serializer to use for which entity, set the "serializer" field in
DatabaseEntity annotation:

    @DatabaseEntity(serializer = MyCustomSerializer.class)
    public class ExampleEntity {
        ...
    }

The storage class will then require you to pass in the mapper because it doesn't have an empty
constructor anymore:

    Mapper<ExampleEntity> mapper = new ExampleEntityMapper(new MyCustomSerializer());

    ExampleEntityStorage storage = SlingerExampleEntityStorage.builder()
        .exampleEntityMapper(mapper)
        .build();

Download
--------

The project is currently in an alpha stage. Will upload it to Maven Central eventually. In the
meantime you have to call the following code to create a local repository:

    ./gradlew build uploadArchives


You could change the build script so the build is placed in your local maven repository, right now
it's placed in the pkg/ folder in the root of the project.

You refer to it this way in gradle:

    compile 'net.daverix.slingerorm:slingerorm-android:0.4'
    provided 'net.daverix.slingerorm:compiler:0.4'

License
-------

    Copyright 2015 David Laurell

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
