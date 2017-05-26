package com.sbg.bdd.wiremock.scoped.common;

import java.lang.reflect.InvocationTargetException;


public class ExceptionSafe {
    public interface SafeFunctor<T>{
        T execute() throws Exception;
    }
    public interface SafeDoer{
        void execute() throws Exception;
    }
    public static <T> T safely(SafeFunctor<T> f){
        try{
            return f.execute();
        }catch(Exception e){
            throw theCauseOf(e);
        }
    }
    public static void doSafeAndSwallow(SafeDoer f){
        swallow(f);
    }
    
    public static void swallow(SafeDoer f) {
        try{
            f.execute();
        }catch(Throwable e){
            System.out.println("Swallowed " + e);
//            //TODO log??
//            e.printStackTrace();
        }
    }
    
    public static void doSafeAndFinally(SafeDoer safe, SafeDoer andFinally){
        try{
            safe.execute();
        }catch(Throwable e){
            throw theCauseOf(e);
        }finally{
            try{
                andFinally.execute();
            }catch (Exception e){
                //do not mask the original exception
                e.printStackTrace();
            }
        }
        
    }
    public static void safely(SafeDoer f){
        doSafely(f);
    }
    public static void doSafely(SafeDoer f){
        try{
            f.execute();
        }catch(Throwable e){
            throw theCauseOf(e);
        }
    }
    
    public static RuntimeException theCauseOf(Throwable e){
        if(e instanceof InvocationTargetException){
            return theCauseOf(((InvocationTargetException) e).getTargetException());
        }else if(e instanceof RuntimeException){
            return (RuntimeException) e;
        }else if(e instanceof Error){
            //just throw it immediately. No recovery from that
            throw (Error) e;
        }else{
            int i = 0;
            while(i < 10 && e.getCause()!=null){
                if(e.getCause() instanceof RuntimeException){
                    return (RuntimeException) e.getCause();
                }else{
                    e=e.getCause();
                }
                i++;
            }
            return new RuntimeException(e);
        }
    }
}
