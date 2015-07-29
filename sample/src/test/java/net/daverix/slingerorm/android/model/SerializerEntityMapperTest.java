package net.daverix.slingerorm.android.model;

import android.content.ContentValues;
import android.database.Cursor;

import net.daverix.slingerorm.android.Mapper;
import net.daverix.slingerorm.android.serialization.TestSerializer;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SerializerEntityMapperTest {
    private Mapper<SerializerEntity> sut;

    @Before
    public void before() {
        sut = new SerializerEntityMapper(new TestSerializer());
    }

    @Test
    public void shouldSetCorrectContentValues() {
        long id = 42;
        Date created = new Date();
        SerializerEntity entity = new SerializerEntity();
        entity.setId(id);
        entity.setCreated(created);

        ContentValues values = mock(ContentValues.class);
        sut.mapValues(entity, values);

        verify(values).put("id", id);
        verify(values).put("created", created.getTime());
    }

    @Test
    public void shouldGetDataFromCursor() {
        long id = 42;
        Date created = new Date();
        String[] fieldNames = new String[] {"id", "created"};

        Cursor cursor = mock(Cursor.class);
        for (int i = 0; i < fieldNames.length; i++) {
            when(cursor.getColumnIndex(fieldNames[i])).thenReturn(i);
        }
        when(cursor.getLong(0)).thenReturn(id);
        when(cursor.getLong(1)).thenReturn(created.getTime());

        SerializerEntity item = new SerializerEntity();
        sut.mapItem(cursor, item);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getCreated()).isEqualTo(created);
    }
}
