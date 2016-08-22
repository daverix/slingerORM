package net.daverix.slingerorm.compiler;


import net.daverix.slingerorm.annotation.ForeignKeyAction;
import net.daverix.slingerorm.annotation.PrimaryKey;
import net.daverix.slingerorm.compiler.mapping.Setter;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public class FieldModel {
    private final Element element;

    private String name;
    private Setter setter;
    private String getterMethod;
    private String annotatedGetterMethod;
    private String setterMethod;
    private String annotatedSetterMethod;
    private boolean primaryKey;
    private FieldAccess getterFieldAccess;
    private FieldAccess setterFieldAccess;
    private ForeignKeyAction foreignKeyUpdateAction;
    private ForeignKeyAction foreignKeyDeleteAction;
    private TypeElement serializer;
    private DatabaseEntityModel databaseEntityModel;

    public FieldModel(Element element) {
        this.element = element;
    }

    public void checkIsPrimaryKey() {
        primaryKey = element.getAnnotation(PrimaryKey.class) != null;
    }

    public TypeElement getSerializer() {
        return serializer;
    }

    public String getGetterMethod() {
        return getterMethod;
    }

    public String getAnnotatedGetterMethod() {
        return annotatedGetterMethod;
    }

    public String getSetterMethod() {
        return setterMethod;
    }

    public String getAnnotatedSetterMethod() {
        return annotatedSetterMethod;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public FieldAccess getGetterFieldAccess() {
        return getterFieldAccess;
    }

    public FieldAccess getSetterFieldAccess() {
        return setterFieldAccess;
    }

    public ForeignKeyAction getForeignKeyUpdateAction() {
        return foreignKeyUpdateAction;
    }

    public ForeignKeyAction getForeignKeyDeleteAction() {
        return foreignKeyDeleteAction;
    }

    public Element getElement() {
        return element;
    }

    public void setAnnotatedSetterMethod(String annotatedSetterMethod) {
        this.annotatedSetterMethod = annotatedSetterMethod;
    }

    public void setSetterMethod(String setterMethod) {
        this.setterMethod = setterMethod;
    }

    public void setGetterMethod(String getterMethod) {
        this.getterMethod = getterMethod;
    }

    public void setAnnotatedGetterMethod(String annotatedGetterMethod) {
        this.annotatedGetterMethod = annotatedGetterMethod;
    }

    public boolean isDatabaseEntity() {
        return databaseEntityModel != null;
    }

    public void setSerializer(TypeElement serializer) {
        this.serializer = serializer;
    }

    public void setGetterFieldAccess(FieldAccess getterFieldAccess) {
        this.getterFieldAccess = getterFieldAccess;
    }

    public void setSetterFieldAccess(FieldAccess setterFieldAccess) {
        this.setterFieldAccess = setterFieldAccess;
    }

    public DatabaseEntityModel getDatabaseEntityModel() {
        return databaseEntityModel;
    }

    public void setDatabaseEntityModel(DatabaseEntityModel databaseEntityModel) {
        this.databaseEntityModel = databaseEntityModel;
    }

    public void setForeignKeyDeleteAction(ForeignKeyAction foreignKeyDeleteAction) {
        this.foreignKeyDeleteAction = foreignKeyDeleteAction;
    }

    public void setForeignKeyUpdateAction(ForeignKeyAction foreignKeyUpdateAction) {
        this.foreignKeyUpdateAction = foreignKeyUpdateAction;
    }

    public Setter getSetter() {
        return setter;
    }

    public void setSetter(Setter setter) {
        this.setter = setter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
