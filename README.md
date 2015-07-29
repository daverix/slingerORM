SlingerORM
==========

SlingerORM is a simple Object Relation Mapper (ORM) focusing on speed and simplicity.

Usage
-----

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
your database table and generates the code for you! The bare minimum you can annotate is this:

    @DatabaseEntity
    public class ExampleEntity {
      @PrimaryKey
      public String Id;
      public String Name;
    }

SlingerORM will first look after getters and setters that match the fields, if it can't find any it
will try to access the fields directly. If you have the fields private with a name that doesn't
match the setter or getter your can annotate the methods to tell SlingerORM which fields are used.
You can also change the name of the field in the database table by annotating the field with
"@FieldName":

    @DatabaseEntity
    public class ExampleEntity {
      @PrimaryKey @FieldName("_id")
      private String mId;
      
      @FieldName("name")
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

SlingerORM generates the code using two annotation processors. The first generates a mapper class
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

To get the implementation simply add "Slinger" before the name of the interface:

    ExampleEntityStorage storage = SlingerExampleEntityStorage.builder().build();

You might wonder where the mapper went? It is used inside the storage implementation and can be set
in the builder if you either want to make your own mapper (a rare case) or you want to provide
a serializer for the mapper (for fields that otherwise can't be mapped):

    ExampleEntityStorage storage = SlingerExampleEntityStorage.builder()
        .exampleEntityMapper(new ExampleEntityMapper())
        .build();

To use a custom serializer, you will have to create a new class and add "@SerializeType" and
"@DeserializeType" annotations:

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

Then in your database entity, you will have to set the serializer in the annotation:

    @DatabaseEntity(serializer = MyCustomSerializer.class)
    public class ExampleEntity {
      @PrimaryKey
      public String Id;
      public Date Created;
    }

The mapper will then require you to pass an instance of this class in the constructor:

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
