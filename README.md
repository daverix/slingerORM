SlingerORM
==========

Serpent is a simple Object Relation Mapper (ORM) focusing on speed and simplicity.


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

When using the mapper you will not call the generated classes directly. The MappingFetcher is used for that. Register your entities and then initialize the MappingFetcher. After a call to initialize get the mapping for a specific entity by calling getMapping(mappingClass):

```
IMappingFetcher mappingFetcher = new MappingFetcher();
mappingFetcher.registerEntity(ExampleEntity.class);
mappingFetcher.initialize();

IMapping mapping = mappingFetcher.getMapping(ExampleEntity.class);
String sql = mapping.getCreateTableSql();
```

If you are developing on Android, there is a class SqliteStorage which will help you with your database interactions. Pass the mapper above to the constructor together with a SQLiteDatabase and call initStorage to create the database table with data from the mapper.

```
IStorage<ExampleEntity> storage = new SQLiteStorage<ComplexEntity>(db, mapper);
storage.initStorage();
ExampleEntity entity = new ExampleEntity();
entity.setName("David");
storage.insert(entity);
```

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
