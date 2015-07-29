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

public class NativeFieldsMapperTest {
    private Mapper<NativeFieldsEntity> sut;

    @Before
    public void before() {
        sut = new NativeFieldsEntityMapper();
    }

    @Test
    public void shouldGetCorrectTableName() {
        assertThat(sut.getTableName()).isEqualTo("NativeFieldsEntity");
    }

    @Test
    public void shouldSetCorrectContentValues() {
        NativeFieldsEntity entity = new NativeFieldsEntity();
        entity.setTypeBoolean(true);
        entity.setTypeString("hello");
        entity.setTypeDouble(1.23456789010111213d);
        entity.setTypeFloat(1.234567f);
        entity.setTypeLong(1234567891011121314L);
        entity.setTypeInt(1337);
        entity.setTypeShort((short) 42);

        ContentValues values = mock(ContentValues.class);

        sut.mapValues(entity, values);

        verify(values).put("typeBoolean", true);
        verify(values).put("typeString", "hello");
        verify(values).put("typeDouble", 1.23456789010111213d);
        verify(values).put("typeFloat", 1.234567f);
        verify(values).put("typeLong", 1234567891011121314L);
        verify(values).put("typeInt", 1337);
        verify(values).put("typeShort", (short) 42);
    }

    @Test
    public void shouldGetCorrectFieldNames() {
        List<String> expected = Arrays.asList("typeBoolean", "typeString", "typeDouble",
                "typeFloat", "typeLong", "typeInt", "typeShort");

        assertThat(sut.getFieldNames()).asList().containsExactlyElementsIn(expected);
    }

    @Test
    public void shouldGetDataFromCursor() {
        Cursor cursor = mock(Cursor.class);
        String[] fieldNames = new String[]{"typeBoolean", "typeString", "typeDouble", "typeFloat",
                "typeLong", "typeInt", "typeShort"};

        for(int i=0;i<fieldNames.length;i++) {
            when(cursor.getColumnIndex(fieldNames[i])).thenReturn(i);
        }
        when(cursor.getShort(0)).thenReturn((short) 1);
        when(cursor.getString(1)).thenReturn("Hello");
        when(cursor.getDouble(2)).thenReturn(1.23456789010111213d);
        when(cursor.getFloat(3)).thenReturn(1.234567f);
        when(cursor.getLong(4)).thenReturn(1234567891011121314L);
        when(cursor.getInt(5)).thenReturn(1337);
        when(cursor.getShort(6)).thenReturn((short) 42);

        NativeFieldsEntity item = new NativeFieldsEntity();
        sut.mapItem(cursor, item);


        assertThat(item.isTypeBoolean()).isTrue();
        assertThat(item.getTypeString()).isEqualTo("Hello");
        assertThat(item.getTypeDouble()).isWithin(0.000000000000000001).of(1.23456789010111213d);
        assertThat(item.getTypeFloat()).isEqualTo(1.234567f);
        assertThat(item.getTypeLong()).isEqualTo(1234567891011121314L);
        assertThat(item.getTypeInt()).isEqualTo(1337);
        assertThat(item.getTypeShort()).isEqualTo((short) 42);
    }
}
