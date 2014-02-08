package net.daverix.slingerorm.android.test.storage;

import net.daverix.slingerorm.android.test.dagger.MappingTestModule;
import net.daverix.slingerorm.android.test.model.ComplexEntity;
import net.daverix.slingerorm.storage.EntityStorage;
import net.daverix.slingerorm.storage.EntityStorageFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import dagger.ObjectGraph;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by daverix on 2/2/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ComplexEntityStorageTest {
    private EntityStorage<ComplexEntity> mStorage;
    
    @Test
    public void shouldRoundtripComplexObject() throws Exception {
        final long expectedId = 42;
        final String expectedName = "David";
        final double expectedValue = 1.831234d;
        final ComplexEntity entity = createEntity(expectedId, expectedName, expectedValue);
        
        mStorage.insert(entity);
        final ComplexEntity actual = mStorage.get(String.valueOf(expectedId));

        assertThat(actual.getId(), is(equalTo(expectedId)));
        assertThat(actual.getEntityName(), is(equalTo(expectedName)));
        assertThat(actual.getValue(), is(equalTo(expectedValue)));
    }

    @Before
    public void setUp() throws Exception {
        ObjectGraph og = ObjectGraph.create(new MappingTestModule());

        EntityStorageFactory storageFactory = og.get(EntityStorageFactory.class);
        mStorage = storageFactory.getStorage(ComplexEntity.class);
    }

    private ComplexEntity createEntity(long id, String name, double value) {
        ComplexEntity entity =new ComplexEntity();
        entity.setId(id);
        entity.setEntityName(name);
        entity.setValue(value);
        return entity;
    }
}
