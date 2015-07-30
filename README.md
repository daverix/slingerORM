SlingerORM
==========

SlingerORM is a simple Object Relation Mapper (ORM) focusing on speed and simplicity.

Introduction
------------

When writing your code for your database layer in Android you will have a lot of code like this:

    public void insert(SQLiteDatabase db, ExampleEntity item) {
        if(db == null) throw new IllegalArgumentException("db is null");
        if(item == null) throw new IllegalArgumentException("item is null");

        ContentValues values = new ContentValues();
        values.put("id", item.getId());
        values.put("name", item.getName());
        db.insertOrThrow("ExampleEntity", null, values);
    }

    public ExampleEntity getEntity(SQLiteDatabase db, long id) {
        Cursor cursor = null;
        try {
            cursor = db.query(false, "ExampleEntity", new String[] {
                "id",
                "name"
            }, "_id = ?", new String[] {
                String.valueOf(id)
            }, null, null, null, "1");

            if(!cursor.moveToFirst()) return null;

            ExampleEntity item = new ExampleEntity();
            item.setId(cursor.getLong(0));
            item.setName(cursor.getString(1));
            return item;
        } finally {
            if(cursor != null) cursor.close();
        }
    }

Writing that becomes tedious when having over hundred fields in your database table that you want to
have mapped. SlingerORM solves this problem by looking at annotations for your class that represents
your database table and generates the code for you!

SlingerORM generates code using two annotation processors. The first generates a mapper class
that you can use to map the tedious part in the example above:

    private Mapper<ExampleEntity> mapper;

    public Storage() {
        this.mapper = new ExampleEntityMapper();
    }

    public void insert(SQLiteDatabase db, ExampleEntity item) {
        if(db == null) throw new IllegalArgumentException("db is null");
        if(item == null) throw new IllegalArgumentException("item is null");

        ContentValues values = new ContentValues();
        mapper.mapValues(item, values);
        db.insertOrThrow(mapper.getTableName(), null, values);
    }

    public ExampleEntity getEntity(SQLiteDatabase db, long id) {
        Cursor cursor = null;
        try {
            cursor = db.query(false, mapper.getTableName(), mapper.getFieldNames(), "_id = ?",
                    new String[] {
                String.valueOf(id)
            }, null, null, null, "1");

            if(!cursor.moveToFirst()) return null;

            ExampleEntity item = new ExampleEntity();
            mapper.mapItem(cursor, item);
            return item;
        } finally {
            if(cursor != null) cursor.close();
        }
    }

It doesn't matter if it was 2 or hundred fields, this takes care of all the mapping for us! But
there are still some tedious code to write! The second annotation processor generates the whole
storage class by implementing your own interface with annotated methods. Here is our storage like
above but with an interface instead:

    @DatabaseStorage
    public interface ExampleEntityStorage {
        @Insert
        void insert(SQLiteDatabase db, ExampleEntity complexEntity);

        @Select(where = "_id = ?")
        ExampleEntity getEntity(SQLiteDatabase db, long id);
    }

The implementation of this interface also creates a builder for setting up the implementation of the
storage class. More about this in the usage section below.

Usage
-----
The bare minimum you need to annotate for a normal class is this:

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


Then if you want a mapper for this entity you only have to instantiate one by suffixing Mapper to
the entity name:

    Mapper<ExampleEntity> exampleEntityMapper = new ExampleEntityMapper();
    ...

You can also generate a storage class that will use one or more mappers by creating an interface and
annotating it with "@DatabaseStorage":

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

To get the implementation of the interface simply add "Slinger" before the name of the interface and
use the builder:

    ExampleEntityStorage storage = SlingerExampleEntityStorage.builder().build();
    ...

If you are using the mapper outside the storage you might not want multiple instances. You can pass
an instance of a mapper in the builder like this:

    ExampleEntityStorage storage = SlingerExampleEntityStorage.builder()
        .exampleEntityMapper(new ExampleEntityMapper())
        .build();
    ...

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

And there you have it! Check the sample module in the source code for more examples.

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

    Copyright 2014 David Laurell

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
