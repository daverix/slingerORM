package net.daverix.slingerorm.android.test.storage;

import net.daverix.slingerorm.Session;
import net.daverix.slingerorm.SessionFactory;
import net.daverix.slingerorm.android.test.dagger.MappingTestModule;
import net.daverix.slingerorm.android.test.model.ComplexEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import dagger.ObjectGraph;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ComplexEntityStorageTest {
    private Session mSession;
    
    @Test
    public void shouldRoundtripComplexObject() throws Exception {
        final long expectedId = 42;
        final String expectedName = "David";
        final double expectedValue = 1.831234d;
        final ComplexEntity entity = createEntity(expectedId, expectedName, expectedValue);
        entity.setIgnoreThisField("ignore this");

        try {
            mSession.beginTransaction();
            mSession.insert(entity);
            mSession.setTransactionSuccessful();
        } finally {
            mSession.endTransaction();
        }

        final ComplexEntity actual = mSession.querySingle(ComplexEntity.class, String.valueOf(expectedId));

        assertThat(actual.getId(), is(equalTo(expectedId)));
        assertThat(actual.getEntityName(), is(equalTo(expectedName)));
        assertThat(actual.getValue(), is(equalTo(expectedValue)));
        assertThat(actual.getIgnoreThisField(), is(nullValue()));
    }

    @Before
    public void setUp() throws Exception {
        ObjectGraph og = ObjectGraph.create(new MappingTestModule());
        SessionFactory factory = og.get(SessionFactory.class);
        mSession = factory.openSession();
        mSession.initTable(ComplexEntity.class);
    }

    private ComplexEntity createEntity(long id, String name, double value) {
        ComplexEntity entity =new ComplexEntity();
        entity.setId(id);
        entity.setEntityName(name);
        entity.setValue(value);
        return entity;
    }
}
