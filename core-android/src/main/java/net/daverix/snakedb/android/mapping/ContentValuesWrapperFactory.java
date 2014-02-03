package net.daverix.snakedb.android.mapping;

public class ContentValuesWrapperFactory implements IContentValuesWrapperFactory {

    public ContentValuesWrapperFactory() {
    }

    @Override
    public IContentValuesWrapper create() {
        return new ContentValuesWrapper();
    }
}
