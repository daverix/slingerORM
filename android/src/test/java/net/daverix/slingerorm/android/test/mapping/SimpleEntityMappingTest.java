package net.daverix.slingerorm.android.test.mapping;

import net.daverix.slingerorm.android.test.model.SimpleEntity;
import net.daverix.slingerorm.mapping.InsertableValues;
import net.daverix.slingerorm.mapping.ResultRow;
import net.daverix.slingerorm.mapping.LazyMappingFetcher;
import net.daverix.slingerorm.mapping.Mapping;
import net.daverix.slingerorm.MappingFetcher;

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

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class SimpleEntityMappingTest {
    private MappingFetcher mMappingFetcher;

    @Test
    public void shouldMapValuesFromSimpleEntity() throws Exception {
        //Arrange
        final String expectedId = "123";
        final String expectedMessage = "hej";
        final int expectedLength = 42;
        final SimpleEntity entity = new SimpleEntity(expectedId, expectedMessage, expectedLength);
        final InsertableValues values = mock(InsertableValues.class);

        //Act
        final Mapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.fetchMapping(SimpleEntity.class);
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

        final ResultRow values = mock(ResultRow.class);
        when(values.getString("id")).thenReturn(expectedId);
        when(values.getString("message")).thenReturn(expectedMessage);
        when(values.getInt("Length")).thenReturn(expectedLength);

        //Act
        final Mapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.fetchMapping(SimpleEntity.class);
        SimpleEntity actual = simpleEntityMapping.map(values);

        //Assert
        assertThat(actual.id, is(equalTo(expectedId)));
        assertThat(actual.message, is(equalTo(expectedMessage)));
        assertThat(actual.Length, is(equalTo(expectedLength)));
    }

    @Test
    public void shouldGetCorrectTableName() throws Exception {
        final String expected = "SimpleEntity";
        final Mapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.fetchMapping(SimpleEntity.class);
        final String actual = simpleEntityMapping.getTableName();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void shouldGetCorrectIdFieldName() throws Exception {
        final String expected = "id";
        final Mapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.fetchMapping(SimpleEntity.class);
        final String actual = simpleEntityMapping.getIdFieldName();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void shouldGetCorrectId() throws Exception {
        final String expected = "321";
        final SimpleEntity entity = new SimpleEntity();
        entity.id = expected;

        final Mapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.fetchMapping(SimpleEntity.class);
        final String actual = simpleEntityMapping.getId(entity);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void shouldGetCorrectTableSql() throws Exception {
        final String expected = "CREATE TABLE IF NOT EXISTS SimpleEntity(id TEXT NOT NULL PRIMARY KEY, message TEXT, Length INTEGER)";

        final Mapping<SimpleEntity> simpleEntityMapping = mMappingFetcher.fetchMapping(SimpleEntity.class);
        final String actual = simpleEntityMapping.getCreateTableSql();

        assertThat(actual, is(equalTo(expected)));
    }

    @Before
    public void setUp() throws Exception {
        mMappingFetcher = new LazyMappingFetcher();
    }
}
