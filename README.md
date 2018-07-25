SlingerORM
==========

SlingerORM is a simple Object Relation Mapper (ORM) for Android that focusing on speed and 
simplicity. It uses code generation to generate code against the database that you don't want to 
write over and over again.

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
        void createTable();

        @Insert
        void insert(ExampleEntity entity);

        @Delete
        void delete(ExampleEntity entity);

        @Select
        List<ExampleEntity> getAllExamples();
    }

Get an instance of your interface by accessing the generated builder by prefixing your interface
name with "Slinger":

    SQLiteDatabase db = ...
    ExampleStorage storage = SlingerExampleStorage
        .builder()
        .database(db)
        .build();

    storage.createTable();

    ExampleEntity entity = new ExampleEntity()
    entity.setId(42);
    entity.setName("David");
    storage.insert(entity);

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

Configuring the storage class
-----------------------------

Here are some of the annotations that can be used on the methods in your storage interface:

    @DatabaseStorage
    public interface ExampleEntityStorage {
        //annotate method with this to create a table based on the given entity class
        @CreateTable(ExampleEntity.class)
        void createTable();

        // slingerORM will run an INSERT INTO statement
        @Insert
        void insert(ExampleEntity exampleEntity);

        // slingerORM will run an UPDATE statement using primary keys in the where clause
        @Update
        void update(ExampleEntity exampleEntity);

        // using an empty delete together with an "@DatabaseEntity" annotated class as parameter 
        // deletes the entity.
        @Delete
        void delete(ExampleEntity exampleEntity);

        // you can also specify a "where" annotation to delete everything that matches the given query
        @Delete @Where("someVar = ?")
        void deleteItemsWithSomeVar(String someVar);

        // slingerorm will match the "?" in order with your method parameters
        @Select @Where("_id = ?")
        ExampleEntity getEntity(long id);

        // not annotating with "@Where" will cause it to use default and query everything
        @Select
        List<ExampleEntity> getAll();

        // @OrderBy and @Limit can be set on the method to add additional data to the sql query
        @Select @OrderBy("created DESC") @Limit("5")
        List<ExampleEntity> getLatest();
    }

Using a custom serializer
-------------------------

Sometimes some type of fields in the database entity will not be a native data type that SlingerORM
supports. You will then need to implement your custom serializer. Create a class that implements 
the Serializer interface and add "@SerializeWith" to the storage interface.

    @DatabaseEntity
    public class ExampleEntity {
        private Date created;
        
        // ...
    }

    @DatabaseStorage
    @SerializeWith(ExampleDateSerializer.class)
    public interface ExampleEntityStorage {
        @Select
        ExampleEntity get(String id);
    }

    public class ExampleDateSerilizer implements Serializer<Date,Long> {
        @Override
        public Long serialize(Date date) {
            return date != null ? date.getTime() : 0;
        }
        
        @Override
        public Date deserialize(long time) {
            return new Date(time);
        }
    }

To tell SlingerORM which serializer to use, set the serializer in the builder:

    ExampleEntityStorage storage = SlingerExampleEntityStorage.builder()
        .exampleDateSerializer(new MyDateSerializer())
        .database(db)
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
    annotationProcessor 'net.daverix.slingerorm:compiler:0.4'

License
-------

    Copyright 2017 David Laurell

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
