package net.daverix.slingerorm.sample.test;

import net.daverix.slingerorm.mapping.IFetchableValues;
import net.daverix.slingerorm.mapping.IInsertableValues;
import net.daverix.slingerorm.mapping.IMapping;
import net.daverix.slingerorm.mapping.IMappingFetcher;
import net.daverix.slingerorm.mapping.MappingFetcher;
import net.daverix.slingerorm.sample.SimpleEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by daverix on 2/1/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class SimpleEntityMappingTest {
    private IMappingFetcher mMappingFetcher;

    @Test
    public void shouldMapValuesFromSimpleEntity() throws Exception {
        //Arrange
        final String expectedId = "123";
        final String expectedMessage = "hej";
        final int expectedLength = 42;
        final SimpleEntity entity = new SimpleEntity(expectedId, expectedMessage, expectedLength);
        final IInsertableValues values = mock(IInsertableValues.class);

        //Act
        final IMapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.getMapping(SimpleEntity.class);
        simpleEntityMapping.mapValues(entity, values);

        //Assert
        verify(values).put("id", expectedId);
        verify(values).put("message", expectedMessage);
        verify(values).put("Length", expectedLength);
    }

    @Test
    public void shouldMapValuesToSimpleEntity() throws Exception {
        //Arrange
        final String expectedId = "123";
        final String expectedMessage = "hej";
        final int expectedLength = 42;

        final IFetchableValues values = mock(IFetchableValues.class);
        when(values.getString("id")).thenReturn(expectedId);
        when(values.getString("message")).thenReturn(expectedMessage);
        when(values.getInt("Length")).thenReturn(expectedLength);

        //Act
        final IMapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.getMapping(SimpleEntity.class);
        SimpleEntity actual = simpleEntityMapping.map(values);

        //Assert
        assertThat(actual.id, is(equalTo(expectedId)));
        assertThat(actual.message, is(equalTo(expectedMessage)));
        assertThat(actual.Length, is(equalTo(expectedLength)));
    }

    @Test
    public void shouldGetCorrectTableName() throws Exception {
        final String expected = "SimpleEntity";
        final IMapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.getMapping(SimpleEntity.class);
        final String actual = simpleEntityMapping.getTableName();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void shouldGetCorrectIdFieldName() throws Exception {
        final String expected = "id";
        final IMapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.getMapping(SimpleEntity.class);
        final String actual = simpleEntityMapping.getIdFieldName();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void shouldGetCorrectId() throws Exception {
        final String expected = "321";
        final SimpleEntity entity = new SimpleEntity();
        entity.id = expected;

        final IMapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.getMapping(SimpleEntity.class);
        final String actual = simpleEntityMapping.getId(entity);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void shouldGetCorrectTableSql() throws Exception {
        final String expected = "CREATE TABLE IF NOT EXISTS SimpleEntity(id TEXT NOT NULL PRIMARY KEY, message TEXT, Length INTEGER)";

        final IMapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.getMapping(SimpleEntity.class);
        final String actual = simpleEntityMapping.getCreateTableSql();

        assertThat(actual, is(equalTo(expected)));
    }

    @Before
    public void setUp() throws Exception {
        mMappingFetcher = new MappingFetcher();
        mMappingFetcher.registerEntity(SimpleEntity.class);
        mMappingFetcher.initialize();
    }
}
