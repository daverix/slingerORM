package net.daverix.slingerorm.annotation;


public enum ForeignKeyAction {
    NO_ACTION,
    RESTRICT,
    SET_NULL,
    SET_DEFAULT,
    CASCADE
}
