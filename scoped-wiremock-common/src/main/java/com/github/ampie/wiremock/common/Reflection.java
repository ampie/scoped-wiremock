package com.github.ampie.wiremock.common;


import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

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
   public static  <T> T invoke(Object target, String methodName, Object ... args) {
        Method[] methods = target.getClass().getDeclaredMethods();
        Method found=null;
        outer:for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if(method.getName().equals(methodName) && parameterTypes.length == args.length){
                for (int i = 0; i < parameterTypes.length; i++) {
                    if(!parameterTypes[i].isInstance(args[i])){
                        continue outer;
                    }
                }
                found=method;
                found.setAccessible(true);
                break outer;
            }
        }
        try {
            return (T) found.invoke(target,args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
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
