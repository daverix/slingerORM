SlingerORM
==========

SlingerORM is a simple Object Relation Mapper (ORM) focusing on speed and simplicity.

Usage
-----

SlingerORM looks at all classes annotated with the DatabaseEntity annotation and then compiles and generates a specific storage class for each of the annotated classes. The bare minimum of an entity looks like this:

    @DatabaseEntity
    public class ExampleEntity {
      @PrimaryKey
      public String Id;
      public String Name;
    }

The DatabaseEntityProcessor will look at all the fields of the class and use the name of the field as a column name in the database table. If you annotate a field with @FieldName("custom name") you can change the name for the column in the database. By default it will use getters and setters starting with set/get and then the field name. This can be changed by annotating your get method with @GetField("fieldName") and your set method with @SetField("fieldName"). If no getters and setters are found and the fields are public, it will use the fields directly. You must always set what field is the primary key. Currently Only a single field is supported for primary key.

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

To save entities to the database, use the provided StorageFactory to get a Storage. The storage has methods for inserting, updating, deleting and querying the database. The SqliteDatabaseConnection is specifically made for Android but you can use the storage in plain java if you implement the DatabaseConnection interface.

    DatabaseConnection db = new SqliteDatabaseConnection(SQLiteDatabase.create(new File("example.db")));
    StorageFactory storageFactory = new GeneratedStorageFactory();
    
    Storage<ExampleEntity> storage = storageFactory.build(ExampleEntity.class);
    storage.createTable(db, ExampleEntity.class);
    
    ExampleEntity entity = new ExampleEntity();
    entity.setName("David");
    try {
        db.beginTransaction();
        storage.insert(db, entity);
        db.setTransactionSuccessful();
    } finally {
        db.endTransaction();
        db.close();    
    }  

If you use Dagger there is a SlingerDaggerModule you can use to get the StorageFactory and all other dependencies:

    ObjectGraph og = ObjectGraph.create(new SlingerDaggerModule());
    StorageFactory storageFactory = og.get(StorageFactory.class);

You should also consider creating your own Dagger Module and get the Storage implementation directly:

    @Module(library = true, includes = SlingerDaggerModule.class)
    public class MyCustomModule {
        @Provides @Singleton
        public Storage<MyEntity> provideMyEntityStorage(StorageFactory factory) {
            return factory.build(MyEntity.class);        
        }
    }

This will make it possible to inject the storage where you need it and remove even more boiler plate code!

Download
--------

The project is currently in an alpha stage. Will upload it to Maven Central eventually. In the meantime you have to call the following code to create a local repository:

    ./gradlew build uploadArchives


You could change the build script so the build is placed in your local maven repository, right now it's placed in the pkg/ folder in the root of the project.

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
