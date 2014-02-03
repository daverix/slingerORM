package net.daverix.slingerorm.mapping;

import net.daverix.slingerorm.exception.FieldNotFoundException;

/**
 * Created by daverix on 2/1/14.
 */
public interface IMapping<T> {
    /**
     * Maps data in item to a {@link IInsertableValues} object.
     * @param item the item with data
     * @throws net.daverix.slingerorm.exception.FieldNotFoundException when field can't be found in values
     */
    public void mapValues(T item, IInsertableValues values) throws FieldNotFoundException;

    /**
     * Creates an object with the data from the database pointer
     * @param values pointer to the database
     * @return an object of the specified template type
     * @throws net.daverix.slingerorm.exception.FieldNotFoundException when field can't be found in values
     */
    public T map(IFetchableValues values) throws FieldNotFoundException;

    /**
     * Returns a create table statement for the template type
     * @return sql query
     */
    public String getCreateTableSql();

    /**
     * Gets the name of the table
     */
    public String getTableName();

    /**
     * Gets the id of the entity
     */
    public String getId(T item);

    /**
     * Gets the name of the id field
     */
    public String getIdFieldName();
}
