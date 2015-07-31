package net.daverix.slingerorm.android.model;

import android.content.ContentValues;
import android.database.MatrixCursor;

import net.daverix.slingerorm.android.Mapper;
import net.daverix.slingerorm.android.serialization.TestSerializer;
import net.daverix.slingerorm.core.android.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE)
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

        ContentValues actual = sut.mapValues(entity);
        assertThat(actual.getAsLong("id")).isEqualTo(id);
        assertThat(actual.getAsLong("created")).isEqualTo(created.getTime());
    }

    @Test
    public void shouldGetDataFromCursor() {
        long id = 42;
        Date created = new Date();
        String[] columnNames = new String[] {"id", "created"};

        MatrixCursor cursor = new MatrixCursor(columnNames);
        cursor.addRow(new Object[]{id, created.getTime()});

        cursor.moveToFirst();
        SerializerEntity item = sut.mapItem(cursor);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getCreated()).isEqualTo(created);
    }
}
