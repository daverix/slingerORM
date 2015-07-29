package net.daverix.slingerorm.android.model;

import android.content.ContentValues;
import android.database.Cursor;

import net.daverix.slingerorm.android.Mapper;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetterSetterEntityMapperTest {
    private Mapper<GetterSetterEntity> sut;

    @Before
    public void before() {
        sut = new GetterSetterEntityMapper();
    }

    @Test
    public void shouldSetCorrectContentValues() {
        String id = "majs";
        int number = 42;
        GetterSetterEntity entity = new GetterSetterEntity();
        entity.setId(id);
        entity.setNumber(42);

        ContentValues values = mock(ContentValues.class);
        sut.mapValues(entity, values);

        verify(values).put("mId", id);
        verify(values).put("mNumber", number);
    }

    @Test
    public void shouldGetCorrectFieldNames() {
        List<String> expected = Arrays.asList("mId", "mNumber");
        assertThat(sut.getFieldNames()).asList().containsExactlyElementsIn(expected);
    }

    @Test
    public void shouldGetDataFromCursor() {
        String id = "apa";
        int number = 42;
        String[] fieldNames = new String[] {"mId", "mNumber"};

        Cursor cursor = mock(Cursor.class);
        for (int i = 0; i < fieldNames.length; i++) {
            when(cursor.getColumnIndex(fieldNames[i])).thenReturn(i);
        }
        when(cursor.getString(0)).thenReturn(id);
        when(cursor.getInt(1)).thenReturn(number);

        GetterSetterEntity item = new GetterSetterEntity();
        sut.mapItem(cursor, item);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getNumber()).isEqualTo(number);
    }
}
