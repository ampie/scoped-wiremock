package com.sbg.bdd.wiremock.scoped.common;


import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Reflection {
    public static <T> T getStaticValue(Class<?> targetClass, String name) {
        try {
            return (T) FieldUtils.readStaticField(targetClass, name, true);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T getValue(Object target, String name) {
        try {
            return (T) FieldUtils.readField(target, name, true);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void setValue(Object target, String name, Object value) {
        try {
            FieldUtils.writeField(target, name, value, true);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
