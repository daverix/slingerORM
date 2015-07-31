package net.daverix.slingerorm.android.model;

import android.content.ContentValues;
import android.database.MatrixCursor;

import net.daverix.slingerorm.android.Mapper;
import net.daverix.slingerorm.core.android.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE)
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

        ContentValues values = sut.mapValues(entity);

        assertThat(values.getAsString("mId")).isEqualTo(id);
        assertThat(values.getAsInteger("mNumber")).isEqualTo(number);
    }

    @Test
    public void shouldGetCorrectColumnNames() {
        List<String> expected = Arrays.asList("mId", "mNumber");
        assertThat(sut.getColumnNames()).asList().containsExactlyElementsIn(expected);
    }

    @Test
    public void shouldGetDataFromCursor() {
        String id = "apa";
        int number = 42;
        String[] columnNames = new String[] {"mId", "mNumber"};
        MatrixCursor cursor = new MatrixCursor(columnNames);
        cursor.addRow(new Object[]{id, number});

        cursor.moveToFirst();
        GetterSetterEntity item = sut.mapItem(cursor);
        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getNumber()).isEqualTo(number);
    }
}
