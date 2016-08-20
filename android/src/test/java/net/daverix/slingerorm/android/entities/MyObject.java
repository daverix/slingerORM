package net.daverix.slingerorm.android.entities;

public class MyObject {
    private final String myValue;

    public MyObject(String myValue) {
        this.myValue = myValue;
    }

    public String getMyValue() {
        return myValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MyObject)) return false;

        MyObject object = (MyObject) o;

        return getMyValue() != null ? getMyValue().equals(object.getMyValue()) : object.getMyValue() == null;

    }

    @Override
    public int hashCode() {
        return getMyValue() != null ? getMyValue().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MyObject{" +
                "myValue='" + myValue + '\'' +
                '}';
    }
}
