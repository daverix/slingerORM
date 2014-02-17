SlingerORM
==========

SlingerORM is a simple Object Relation Mapper (ORM) focusing on speed and simplicity.


Usage
-----

SlingerORM looks at all classes annotated with the DatabaseEntity annotation and then compiles and generates a specific mapper class for each of the annotated classes. The bare minimum of an entity looks like this:

```
@DatabaseEntity
public class ExampleEntity {
  @PrimaryKey
  public String Id;
  public String Name;
}
```
The DatabaseEntityProcessor will look at all the fields of the class and use the name of the field as a column name in the database table. If you annotate a field with @FieldName("custom name") you can change the name for the column in the database. By default it will use getters and setters starting with set/get and then the field name. This can be changed by annotating your get method with @GetField("fieldName") and your set method with @SetField("fieldName"). If no getters and setters are found and the fields are public, it will use the fields directly. You must always set what field is the primary key. Currently Only a single field is supported for primary key.

```
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
```

When using the mapper you will not call the generated classes directly. The MappingFetcher is used for that. When calling fetchMapping(mappingClass) it will first see if the mapping have been called before. If it exists in memory then it will just returns it, otherwise it will initiate the generated mapper class for the specific entity class and then return it (The lazy approach):

```
MappingFetcher mappingFetcher = new LazyMappingFetcher();
Mapping<ExampleEntity> mapping = mappingFetcher.fetchMapping(ExampleEntity.class);
String sql = mapping.getCreateTableSql();
```

There is also an interface called EntityStorage that you can use to store your entities to the database and query them. Right now there is only an implementation for Android called SQLiteStorage that implements this interface. You can provide the required dependencies for SQLiteStorage like this:

```
SQLiteDatabaseReference dbReference = new ExampleSQLiteDatabaseHelper();
MappingFetcher mappingFetcher = new LazyMappingFetcher();
InsertableContentValuesFactory insertableContentValuesFactory = new ContentValuesWrapperFactory();
FetchableCursorValuesFactory fetchableCursorValuesFactory = new CursorWrapperFactory();

EntityStorageFactory storageFactory = new SQLiteStorageFactory(SQLiteDatabaseReference dbReference,
                                                                      MappingFetcher mappingFetcher,
                                                                      InsertableContentValuesFactory insertableContentValuesFactory,
                                                                      FetchableCursorValuesFactory fetchableCursorValuesFactory);

EntityStorage<ExampleEntity> storage = storageFactory.getStorage(ComplexEntity.class);

ExampleEntity entity = new ExampleEntity();
entity.setName("David");
storage.insert(entity);
```

..or you can use the provided Dagger module to inject the dependencies:

```
ObjectGraph og = ObjectGraph.create(new ExampleModule());
EntityStorageFactory storageFactory = og.get(EntityStorageFactory.class);
EntityStorage<ExampleEntity> storage = storageFactory.getStorage(ComplexEntity.class);

ExampleEntity entity = new ExampleEntity();
entity.setName("David");
storage.insert(entity);
```

Using dagger you must provide your own dagger module that provides an implementation for SQLiteDatabaseReference:

```
@Module(includes = MappingModule.class)
public class ExampleModule {
    @Provides
    public SQLiteDatabaseReference provideSQLiteDatabaseReference(ExampleSQLiteDatabaseHelper dbReference) {
        return dReference;
    }
}

```

Download
--------

The project is currently in an alpha stage. Will upload it to Maven Central eventually. In the meantime you have to call the following code to create a local repository:

```
./gradlew build uploadArchives
```

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
