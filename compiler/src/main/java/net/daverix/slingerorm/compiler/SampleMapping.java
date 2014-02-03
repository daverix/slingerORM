package net.daverix.slingerorm.compiler;

import net.daverix.slingerorm.exception.FieldNotFoundException;
import net.daverix.slingerorm.mapping.IFetchableValues;
import net.daverix.slingerorm.mapping.IInsertableValues;
import net.daverix.slingerorm.mapping.IMapping;

/**
 * Created by daverix on 2/1/14.
 */
public class SampleMapping implements IMapping<Sample> {
    @Override
    public void mapValues(Sample item, IInsertableValues values) {
        if(item == null) throw new IllegalArgumentException("item is null");
        if(values == null) throw new IllegalArgumentException("values is null");

        values.put("Id", item.Id);
    }

    @Override
    public Sample map(IFetchableValues values) throws FieldNotFoundException {
        Sample sample = new Sample();
        sample.Id = values.getString("Id");
        return sample;
    }

    @Override
    public String getCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS Sample(Id NOT NULL PRIMARY KEY)";
    }

    @Override
    public String getTableName() {
        return "Sample";
    }

    @Override
    public String getId(Sample item) {
        return item.Id;
    }

    @Override
    public String getIdFieldName() {
        return "Id";
    }
}
