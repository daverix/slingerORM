SlingerORM
==========

SlingerORM is a simple Object Relation Mapper (ORM) focusing on speed and simplicity. It uses code
generation to generate code against the database that you don't want to write over and over again.

Basic usage
----------

The bare minimum that is needed for SlingerORM to work is an entity written by you:

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

Then a mapper class will be generated. The name of the mapper class is your entity's name and
"Mapper" suffixed to it:

    Mapper<ExampleEntity> exampleEntityMapper = new ExampleEntityMapper();

To make database operations in Android, you use the SlingerStorage class like this:

    SQLiteDatabase db = ...
    SlingerStorage storage = new SlingerStorage(db);
    storage.registerMapper(ExampleEntity.class, new ExampleEntityMapper());

    storage.createTable(ExampleEntity.class);

    ExampleEntity entity = new ExampleEntity()
    entity.setId(42);
    entity.setName("David");
    storage.insert(entity);

    List<ExampleEntity> examples = storage.select(ExampleEntity.class).where("name LIKE ?", "D%").toList();
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
can annotate these fields with @ColumnName:

    @DatabaseEntity
    public class ExampleEntity {
      @ColumnName("Id") @PrimaryKey
      private String id;
      @ColumnName("Name")
      private String name;

      ...
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

The mapper class will then require you to pass in the serializer because it doesn't have an empty
constructor anymore:

    Mapper<ExampleEntity> mapper = new ExampleEntityMapper(new MyCustomSerializer());

    SlingerStorage storage = new SlingerStorage(db);
    storage.registerMapper(ExampleEntity.class, mapper);
    ...

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
