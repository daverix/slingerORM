package net.daverix.slingerorm.android.model;

import android.database.sqlite.SQLiteDatabase;

import net.daverix.slingerorm.android.BuildConfig;
import net.daverix.slingerorm.android.Mapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE)
public class MultipleKeyEntityMapperTest {
    private Mapper<MultipleKeyEntity> sut;

    @Before
    public void setUp() {
        sut = MultipleKeyEntityMapper.create();
    }

    @Test
    public void createTableSqlShouldNotThrowException() {
        String sql = sut.createTable();
        SQLiteDatabase.create(null).execSQL(sql);
    }

    @Test
    public void createSqlContainsPrimaryKeys() {
        String actual = sut.createTable();

        assertThat(actual).contains("groupId");
        assertThat(actual).contains("userId");
        assertThat(actual).contains("PRIMARY KEY");
    }

    @Test
    public void itemQueryContainsPrimaryKeys() {
        String actual = sut.getItemQuery();

        assertThat(actual).contains("groupId");
        assertThat(actual).contains("userId");
    }

    @Test
    public void itemArgumentsAreCorrect() {
        MultipleKeyEntity entity = new MultipleKeyEntity();
        entity.setUserId("myUser");
        entity.setGroupId("myGroup");

        String[] actual = sut.getItemQueryArguments(entity);

        assertThat(Arrays.asList(actual)).containsExactly("myUser", "myGroup");
    }
}