snakedb
=======

A simple Object Relation Mapper (ORM) focusing on speed and simplicity.


Usage
-----

SnakeDB looks at all classes annotated with the DatabaseEntity annotation and then compiles and generates a specific mapper class for each of the annotated classes. The bare minimum of an entity looks like this:

```
@DatabaseEntity
public class ExampleEntity {
  @PrimaryKey
  public String Id;
  public String Name;
}
```
The DatabaseEntityProcessor will look at all the public fields and use the name of the field as a column name in the database table. If you annotate a field with @FieldName("custom name") you can change the name for the column in the database. By default it will use getters and setters starting with set/get and then the field name. This can be changed by annotating your get method with @GetField("fieldName") and your set method with @SetField("fieldName"). You must always set what field is the primary key. Currently Only a single field is supported for primary key.

When using the mapper you will not call the generated classes directly. The MappingFetcher is used for that. Register your entities and then initialize the MappingFetcher. After a call to initialize get the mapping for a specific entity by calling getMapping(mappingClass):

```
IMappingFetcher mappingFetcher = new MappingFetcher();
mappingFetcher.registerEntity(ExampleEntity.class);
mappingFetcher.initialize();

IMapping mapping = mappingFetcher.getMapping(ExampleEntity.class);
String sql = mapping.getCreateTableSql();
```

If you are developing on Android, the simplest way to interact with your database is to use the SQLStorageFactory built specifically for Android and it's SQLiteDatabase class.

```
IStorage<ExampleEntity> storage = SQliteStorageFactory.initStorage(db, ExampleEntity.class);
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
