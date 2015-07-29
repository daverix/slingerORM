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

public class CustomFieldEntityMapperTest {
    private Mapper<CustomFieldEntity> sut;

    @Before
    public void before() {
        sut = new CustomFieldEntityMapper();
    }

    @Test
    public void shouldSetCorrectContentValues() {
        String id = "dl";
        String name = "David Laurell";
        CustomFieldEntity entity = new CustomFieldEntity();
        entity.setId(id);
        entity.setName(name);

        ContentValues values = mock(ContentValues.class);
        sut.mapValues(entity, values);

        verify(values).put("Id", id);
        verify(values).put("Name", name);
    }

    @Test
    public void shouldGetCorrectFieldNames() {
        List<String> expected = Arrays.asList("Id", "Name");
        assertThat(sut.getFieldNames()).asList().containsExactlyElementsIn(expected);
    }

    @Test
    public void shouldGetDataFromCursor() {
        String id = "banana";
        String name = "Code Monkey";
        String[] fieldNames = new String[] {"Id", "Name"};

        Cursor cursor = mock(Cursor.class);
        for (int i = 0; i < fieldNames.length; i++) {
            when(cursor.getColumnIndex(fieldNames[i])).thenReturn(i);
        }
        when(cursor.getString(0)).thenReturn(id);
        when(cursor.getString(1)).thenReturn(name);

        CustomFieldEntity item = new CustomFieldEntity();
        sut.mapItem(cursor, item);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getName()).isEqualTo(name);
    }
}
