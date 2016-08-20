package net.daverix.slingerorm.android.mapper;

import android.content.ContentValues;
import android.database.MatrixCursor;

import net.daverix.slingerorm.android.BuildConfig;
import net.daverix.slingerorm.android.Mapper;
import net.daverix.slingerorm.android.entities.CustomFieldEntity;
import net.daverix.slingerorm.android.entities.ForeignEntity;
import net.daverix.slingerorm.android.entities.ForeignEntityMapper;

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
public class ForeignEntityMapperTest {
    private Mapper<ForeignEntity> sut;

    @Before
    public void before() {
        sut = new ForeignEntityMapper();
    }

    @Test
    public void shouldSetCorrectContentValues() {
        String id = "majs";
        CustomFieldEntity customFieldEntity = new CustomFieldEntity();
        customFieldEntity.setId("hej");
        customFieldEntity.setName("då");

        ForeignEntity entity = new ForeignEntity();
        entity.setId(id);
        entity.setCustom(customFieldEntity);

        ContentValues values = sut.mapValues(entity);
        assertThat(values.getAsString("id")).isEqualTo(id);
        assertThat(values.getAsString("custom_id")).isEqualTo("hej");
    }

    @Test
    public void shouldGetCorrectColumnNames() {
        List<String> expected = Arrays.asList("id", "custom_id");
        assertThat(sut.getColumnNames()).asList().containsExactlyElementsIn(expected);
    }

    @Test
    public void shouldGetDataFromCursor() {
        String id = "apa";
        String customId = "hej";
        String customName = "då";
        String[] columnNames = new String[] {"id", "custom_id", "custom_id_Id", "custom_id_Name"};
        MatrixCursor cursor = new MatrixCursor(columnNames);
        cursor.addRow(new Object[]{id, customId, customId, customName});

        cursor.moveToFirst();
        ForeignEntity item = sut.mapItem(cursor);
        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getCustom()).isNotNull();
        assertThat(item.getCustom().getId()).isEqualTo(customId);
        assertThat(item.getCustom().getName()).isEqualTo(customName);
    }
}
